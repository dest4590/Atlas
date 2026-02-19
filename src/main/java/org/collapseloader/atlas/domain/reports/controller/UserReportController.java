package org.collapseloader.atlas.domain.reports.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.reports.dto.CreateReportRequest;
import org.collapseloader.atlas.domain.reports.dto.ResolveReportRequest;
import org.collapseloader.atlas.domain.reports.dto.UserReportResponse;
import org.collapseloader.atlas.domain.reports.entity.ReportStatus;
import org.collapseloader.atlas.domain.reports.service.UserReportService;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserReportController {
    private final UserReportService reportService;

    @PostMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserReportResponse>> createReport(
            @AuthenticationPrincipal User reporter,
            @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportService.createReport(reporter, request)));
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserReportResponse>>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllReports(status, pageable)));
    }

    @PutMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserReportResponse>> resolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportService.resolveReport(id, admin, request)));
    }
}
