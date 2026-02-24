package org.collapseloader.atlas.domain.crashlogs.repository;

import org.collapseloader.atlas.domain.crashlogs.entity.CrashLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CrashLogRepository extends JpaRepository<CrashLog, Long> {
    @Modifying
    @Query("delete from CrashLog cl where cl.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
