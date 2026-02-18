package org.collapseloader.atlas.domain.reports.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.reports.dto.CreateReportRequest;
import org.collapseloader.atlas.domain.reports.dto.ResolveReportRequest;
import org.collapseloader.atlas.domain.reports.dto.UserReportResponse;
import org.collapseloader.atlas.domain.reports.entity.ReportStatus;
import org.collapseloader.atlas.domain.reports.entity.UserReport;
import org.collapseloader.atlas.domain.reports.repository.UserReportRepository;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserReportService {
    private final UserReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserReportResponse createReport(User reporter, CreateReportRequest request) {
        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Reported user not found"));

        UserReport report = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        return mapToResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public Page<UserReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        Page<UserReport> reports;
        if (status != null) {
            reports = reportRepository.findAllByStatus(status, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }
        return reports.map(this::mapToResponse);
    }

    @Transactional
    public UserReportResponse resolveReport(Long reportId, User admin, ResolveReportRequest request) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        report.setStatus(request.getStatus());
        report.setAdminNotes(request.getAdminNotes());
        report.setResolvedBy(admin);
        report.setResolvedAt(Instant.now());

        return mapToResponse(reportRepository.save(report));
    }

    private UserReportResponse mapToResponse(UserReport report) {
        return UserReportResponse.builder()
                .id(report.getId())
                .reporter(new UserReportResponse.ReporterDto(report.getReporter().getId(), report.getReporter().getUsername()))
                .reportedUser(new UserReportResponse.ReporterDto(report.getReportedUser().getId(), report.getReportedUser().getUsername()))
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedBy(report.getResolvedBy() != null ?
                        new UserReportResponse.ReporterDto(report.getResolvedBy().getId(), report.getResolvedBy().getUsername()) : null)
                .resolvedAt(report.getResolvedAt())
                .adminNotes(report.getAdminNotes())
                .build();
    }
}
