package com.capitalone.dashboard.collector;

import static com.capitalone.dashboard.model.CollectorType.Build;
import static com.capitalone.dashboard.model.CollectorType.SCM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.TravisCiCollector;
import com.capitalone.dashboard.model.TravisCiJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.TravisCiCollectorRepository;
import com.capitalone.dashboard.repository.TravisCiJobRepository;

/**
 * CollectorTask that fetches Build information from TravisCi
 */
@org.springframework.stereotype.Component
public class TravisCiCollectorTask extends CollectorTask<TravisCiCollector> {

	private final TravisCiCollectorRepository travisCiCollectorRepository;
	private final TravisCiJobRepository travisCiJobRepository;
	private final BuildRepository buildRepository;
	private final TravisCiClient travisCiClient;
	private final TravisCiSettings travisCiSettings;
	private final ComponentRepository dbComponentRepository;

	@Autowired
	public TravisCiCollectorTask(TaskScheduler taskScheduler, TravisCiCollectorRepository travisCiCollectorRepository,
			TravisCiJobRepository travisCiJobRepository, BuildRepository buildRepository, TravisCiClient travisCiClient,
			TravisCiSettings travisCiSettings, ComponentRepository dbComponentRepository) {
		super(taskScheduler, "Travis-CI");
		this.travisCiCollectorRepository = travisCiCollectorRepository;
		this.travisCiJobRepository = travisCiJobRepository;
		this.buildRepository = buildRepository;
		this.travisCiClient = travisCiClient;
		this.travisCiSettings = travisCiSettings;
		this.dbComponentRepository = dbComponentRepository;
	}

	@Override
	public TravisCiCollector getCollector() {
		return new TravisCiCollector();
	}

	@Override
	public BaseCollectorRepository<TravisCiCollector> getCollectorRepository() {
		return travisCiCollectorRepository;
	}

	@Override
	public String getCron() {
		return travisCiSettings.getCron();
	}

	@Override
	public void collect(TravisCiCollector collector) {
		// FIXME very funny transactional boundaries, have to check in terms of
		// mongo
		long start = System.currentTimeMillis();
		ObjectId collectorId = collector.getId();
		log("Collecting jobs", start);
		List<TravisCiJob> jobs = travisCiJobRepository.findByCollectorId(collectorId);
		List<Component> componentsForTravis = collectCandidateRepositories(collector);
		Map<ObjectId, TravisCiJob> jobLookup = createComponentIdVsJobLookup(jobs);
		log("Collected components " + componentsForTravis.size(), start);
		for (Component component : componentsForTravis) {
			TravisCiJob job = jobLookup.get(component.getId());
			if (job == null) {
				job = createNewJob(collectorId, component.getId());
			}
			if (shouldProcess(job.getLastUpdated())) {
				List<CollectorItem> scms = component.getCollectorItems(SCM);
				ObjectId travisCollectorItemId = component.getCollectorItems(Build).get(0).getId();
				for (CollectorItem scm : scms) {
					Map<String, Object> scmOptions = scm.getOptions();
					if (scmOptions != null && scmOptions.get("url") != null) {
						String repoUrl = (String) scmOptions.get("url");
						log("Processing builds for repo url " + repoUrl);
						addNewBuilds(repoUrl, job.getLastUpdated(), travisCollectorItemId);
					}
				}
				job.setLastUpdated(System.currentTimeMillis());
				travisCiJobRepository.save(job);
			} else {
				log("Skipping component " + component.getName());
			}
		}
		log("Finished", start);
	}

	private Map<ObjectId, TravisCiJob> createComponentIdVsJobLookup(List<TravisCiJob> jobs) {
		Map<ObjectId, TravisCiJob> lookup = new HashMap<>();
		for (TravisCiJob job : jobs) {
			lookup.put(job.getComponentId(), job);
		}
		return lookup;
	}

	public void onStartup() {
		super.onStartup();
		log("Firing non-scheduled run");
		this.run();
	}

	private boolean shouldProcess(long lastBuildUpdated) {
		// if build was updated recently don't update
		return (System.currentTimeMillis() - lastBuildUpdated) > travisCiSettings.getCheckAfterMillis();
	}

	private TravisCiJob createNewJob(ObjectId collectorId, ObjectId componentId) {
		TravisCiJob job = new TravisCiJob();
		job.setDescription("public-urls");
		job.setLastUpdated(0l);
		job.setEnabled(true);
		job.setCollectorId(collectorId);
		job.setComponentId(componentId);
		travisCiJobRepository.save(job);
		return job;
	}

	private void addNewBuilds(String repoUrl, long lastUpdated, ObjectId collectorItemId) {
		List<Build> buildDetails = travisCiClient.getBuildDetails(repoUrl);
		for (Build buildDetail : buildDetails) {
			if (buildDetail.getStartTime() > lastUpdated) {
				buildDetail.setCollectorItemId(collectorItemId);
				buildRepository.save(buildDetail);
			}
		}
	}

	private List<Component> collectCandidateRepositories(Collector collector) {
		List<Component> components = new ArrayList<>();
		for (Component comp : dbComponentRepository.findAll()) {
			if (comp.getCollectorItems() != null && !comp.getCollectorItems().isEmpty()) {
				if (isRelevant(comp.getCollectorItems().get(CollectorType.Build), collector)) {
					components.add(comp);
				}
			}
		}
		return components;
	}

	private boolean isRelevant(List<CollectorItem> itemList, Collector collector) {
		if (itemList != null) {
			for (CollectorItem ci : itemList) {
				if (ci != null && ci.getCollectorId().equals(collector.getId())) {
					return true;
				}
			}
		}
		return false;
	}
}
