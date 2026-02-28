package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnalyticsCounterRepository extends JpaRepository<AnalyticsCounter, Long> {
    Optional<AnalyticsCounter> findByKey(String key);

    @Modifying
    @Query("UPDATE AnalyticsCounter c SET c.value = c.value + 1 WHERE c.key = :key")
    int incrementValueByKey(@Param("key") String key);
}
