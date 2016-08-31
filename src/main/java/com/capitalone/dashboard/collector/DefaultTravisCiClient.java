package com.capitalone.dashboard.collector;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.util.Supplier;


/**
 * TravisCiClient implementation that uses RestTemplate and JSONSimple to
 * fetch information from TravisCi instances.
 */
@Component
public class DefaultTravisCiClient implements TravisCiClient {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultTravisCiClient.class);

    private final RestOperations rest;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    public DefaultTravisCiClient(Supplier<RestOperations> restOperationsSupplier) {
        this.rest = restOperationsSupplier.get();
    }


    @Override
    public List<Build> getBuildDetails(String repoUrl) {
    	List<Build> buildDetails = new ArrayList<Build>();
        try {
            String buildUrl = buildRestURL(repoUrl);
            ResponseEntity<String> result = makeRestCall(buildUrl);
            String resultJSON = result.getBody();
            if (StringUtils.isEmpty(resultJSON)) {
                LOG.error("Error getting build details for URL =" + buildUrl);
            } else {
            	JSONParser parser = new JSONParser();
            	try {
            		JSONObject buildsJson = (JSONObject) parser.parse(resultJSON);
            		JSONArray builds = (JSONArray) buildsJson.get("builds");
            		for (Object rawBuild : builds) {
            			JSONObject buildJson = (JSONObject) rawBuild;
            			Build build = getBuildDetail(buildJson);
            			// addChangeSets(build, buildJson);
            			buildDetails.add(build);
            		}
            	} catch (ParseException | java.text.ParseException e) {
            		LOG.error("Parsing build: " + buildUrl, e);
            	}
            }
        } catch (RestClientException | MalformedURLException  exc) {
            LOG.error("Exception loading build details: " + exc.getMessage() + ". URL =" + repoUrl, exc );
        }
        return buildDetails;
    }


	private Build getBuildDetail(JSONObject buildJson) throws java.text.ParseException {
		Build build = new Build();
		build.setNumber(getString(buildJson, "number"));
		build.setBuildUrl(getString(buildJson,"@href"));
		build.setTimestamp(System.currentTimeMillis());
		build.setStartTime(getTime(buildJson, "started_at"));
		build.setDuration((Long) buildJson.get("duration"));
		build.setEndTime(getTime(buildJson, "finished_at"));
		build.setBuildStatus(getBuildStatus(buildJson, "state"));
		return build;
	}


	protected long getTime(JSONObject buildJson, String fieldName) throws java.text.ParseException {
		return dateFormatter.parse(getString(buildJson, fieldName)).getTime();
	}


	protected String buildRestURL(String repoUrl) {
		String[] splits = repoUrl.split("/");
		if (splits.length < 2) {
			throw new IllegalArgumentException("Expected a valid repo url, but got "+repoUrl);
		}
		int totalLength = splits.length;
		// pick up the last 2
		return "https://api.travis-ci.org/repo/"+splits[totalLength - 2 ] + "%2F"+ splits[totalLength - 1 ] + "/builds";
	}



    private String getString(JSONObject json, String key) {
        return (String) json.get(key);
    }

    private BuildStatus getBuildStatus(JSONObject buildJson, String key) {
        String status = buildJson.get(key).toString().toUpperCase();
        switch (status) {
            case "SUCCESS":
            case "PASSED":
                return BuildStatus.Success;
            case "UNSTABLE":
                return BuildStatus.Unstable;
            case "FAILURE":
            case "FAILED":
                return BuildStatus.Failure;
            case "ABORTED":
                return BuildStatus.Aborted;
            default:
            	LOG.warn("TravisClient could not understand status : "+ status);
                return BuildStatus.Unknown;
        }
    }

    protected ResponseEntity<String> makeRestCall(String sUrl) throws MalformedURLException {
        URI thisuri = URI.create(sUrl);
        return rest.exchange(thisuri, HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

    }

    protected HttpHeaders createHeaders() {
    	// -H 'Travis-API-Version: 3'  -H 'Content-Type: application/json' -H 'Accept: application/json'
        HttpHeaders headers = new HttpHeaders();
        headers.set("Travis-API-Version", "3");
        headers.set("Accept", "application/json");
        return headers;
    }

    protected String getLog(String buildUrl) {
        try {
            return makeRestCall(joinURL(buildUrl, "consoleText")).getBody();
        } catch (MalformedURLException mfe) {
            LOG.error("malformed url for build log", mfe);
        }

        return "";
    }

    // join a base url to another path or paths - this will handle trailing or non-trailing /'s
    public static String joinURL(String base, String... paths) throws MalformedURLException {
        StringBuilder result = new StringBuilder(base);
        for (String path : paths) {
            String p = path.replaceFirst("^(\\/)+", "");
            if (result.lastIndexOf("/") != result.length() - 1) {
                result.append('/');
            }
            result.append(p);
        }
        return result.toString();
    }
}
