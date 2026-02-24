package org.collapseloader.atlas.domain.crashlogs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.crashlogs.dto.CreateCrashLogRequest;
import org.collapseloader.atlas.domain.crashlogs.dto.CrashLogResponse;
import org.collapseloader.atlas.domain.crashlogs.service.CrashLogService;
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
public class CrashLogController {
    private final CrashLogService crashLogService;

    @PostMapping("/crash-logs")
    public ResponseEntity<ApiResponse<CrashLogResponse>> createCrashLog(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCrashLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(crashLogService.createCrashLog(user, request)));
    }

    @GetMapping("/admin/crash-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<CrashLogResponse>>> getCrashLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(crashLogService.getCrashLogs(pageable)));
    }
}
