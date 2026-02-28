package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.AnalyticsClientRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsClientRepostiory extends JpaRepository<AnalyticsClientRecord, Long> {
    void deleteByLaunchTimestampBefore(Long timestamp);
}
