package com.capitalone.dashboard.collector;

import java.util.List;

import com.capitalone.dashboard.model.Build;

/**
 * Client for fetching job and build information from TravisCi
 */
public interface TravisCiClient {

    /**
     * Fetch full populated build information for a build.
     *
     * @param repodUrl the url of the repo
     * @return List of Build instanes
     */
    List<Build> getBuildDetails(String repoUrl);
    
}
