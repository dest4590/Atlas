package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsServerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsServerRepository extends JpaRepository<AnalyticsServerRecord, Long> {
    Optional<Boolean> existsByDomain(String domain);

    AnalyticsServerRecord findByDomain(String domain);

    List<AnalyticsServerRecord> findAllByOrderByJoinCountDesc();

    // find joinCount where joinCount > 0
    void deleteAllByJoinCountLessThan(Long joinCount);

    void deleteAllByJoinCount(Long joinCount);
}
