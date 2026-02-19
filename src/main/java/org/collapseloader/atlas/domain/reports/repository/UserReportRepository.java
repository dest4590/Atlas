package org.collapseloader.atlas.domain.reports.repository;

import org.collapseloader.atlas.domain.reports.entity.ReportStatus;
import org.collapseloader.atlas.domain.reports.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    @Modifying
    @Query("delete from UserReport ur where ur.reporter.id = :userId or ur.reportedUser.id = :userId or ur.resolvedBy.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    Page<UserReport> findAllByStatus(ReportStatus status, Pageable pageable);

    Page<UserReport> findAllByReportedUserId(Long reportedUserId, Pageable pageable);
}
