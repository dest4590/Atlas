package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsServerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsServerRepository extends JpaRepository<AnalyticsServerRecord, Long> {
    boolean existsByDomain(String domain);

    AnalyticsServerRecord findByDomain(String domain);

    @Modifying
    @Query("UPDATE AnalyticsServerRecord s SET s.joinCount = s.joinCount + 1 WHERE s.domain = :domain")
    int incrementJoinCountByDomain(@Param("domain") String domain);

    List<AnalyticsServerRecord> findAllByOrderByJoinCountDesc();

    // find joinCount where joinCount > 0
    void deleteAllByJoinCountLessThan(Long joinCount);

    void deleteAllByJoinCount(Long joinCount);
}
