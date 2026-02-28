package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsClientRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsClientRepostiory extends JpaRepository<AnalyticsClientRecord, Long> {
    void deleteByLaunchTimestampBefore(Long timestamp);

    List<AnalyticsClientRecord> findTop200ByOrderByLaunchTimestampDesc();

    List<AnalyticsClientRecord> findAllByLaunchTimestampBetweenOrderByLaunchTimestampAsc(Long from, Long to);
}
