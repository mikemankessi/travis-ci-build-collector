package com.capitalone.dashboard.model;

import org.bson.types.ObjectId;

/**
 * CollectorItem extension to store the instance, build job and build url.
 */
public class TravisCiJob extends CollectorItem {
    private static final String COMPONENT_ID = "componentId";
    
    public TravisCiJob() {
    }

    public ObjectId getComponentId() {
        return (ObjectId) getOptions().get(COMPONENT_ID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }

        TravisCiJob buildJob = (TravisCiJob) o;

        return getComponentId().equals(buildJob.getComponentId());
    }

    @Override
    public int hashCode() {
        int result = getComponentId().hashCode();
        return result;
    }

	public void setComponentId(ObjectId componentId) {
		getOptions().put(COMPONENT_ID, componentId);
	}
}
