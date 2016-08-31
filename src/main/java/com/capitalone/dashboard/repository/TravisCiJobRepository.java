package com.capitalone.dashboard.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.TravisCiJob;

public interface TravisCiJobRepository extends BaseCollectorItemRepository<TravisCiJob> {
    /**
     * Finds the {@link CollectorItem} for a given collector. This should represent a unique
     * instance of a {@link CollectorItem} for a given {@link com.capitalone.dashboard.model.Collector}.
     *
     * @param collectorId {@link com.capitalone.dashboard.model.Collector} id
     * @return a {@link CollectorItem}
     */
    @Query(value="{ 'collectorId' : ?0}")
    List<TravisCiJob> findByCollectorId(ObjectId collectorId);

}
