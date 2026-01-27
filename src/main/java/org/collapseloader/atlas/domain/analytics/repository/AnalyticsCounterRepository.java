package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalyticsCounterRepository extends JpaRepository<AnalyticsCounter, Long> {
    Optional<AnalyticsCounter> findByKey(String key);
}
