package com.capitalone.dashboard.model;

/**
 * Extension of Collector that stores current build server configuration.
 */
public class TravisCiCollector extends Collector {
    public TravisCiCollector() {
        this.setName("Travis-CI");
        this.setCollectorType(CollectorType.Build);
        this.setOnline(true);
        this.setEnabled(true);
    }
}
