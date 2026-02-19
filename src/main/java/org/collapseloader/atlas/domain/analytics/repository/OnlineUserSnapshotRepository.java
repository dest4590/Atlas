package org.collapseloader.atlas.domain.analytics.repository;

import org.collapseloader.atlas.domain.analytics.entity.OnlineUserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OnlineUserSnapshotRepository extends JpaRepository<OnlineUserSnapshot, Long> {
    List<OnlineUserSnapshot> findAllByTimestampAfterOrderByTimestampAsc(Instant timestamp);
}
