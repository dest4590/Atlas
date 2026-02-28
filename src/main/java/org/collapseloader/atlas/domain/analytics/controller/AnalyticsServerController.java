package org.collapseloader.atlas.domain.analytics.controller;

import jakarta.validation.Valid;
import org.collapseloader.atlas.domain.analytics.dto.request.AnalyticsServerRecordRequest;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsServerService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/servers")
public class AnalyticsServerController {
    private final AnalyticsServerService serverService;

    public AnalyticsServerController(AnalyticsServerService serverService) {
        this.serverService = serverService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<String>> joinAnalyticsServer(@Valid @RequestBody AnalyticsServerRecordRequest request) {
        try {
            serverService.recordServerJoin(request.server());
            return ResponseEntity.ok(ApiResponse.success("Server join recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error("Failed to record server join"));
        }
    }
}
