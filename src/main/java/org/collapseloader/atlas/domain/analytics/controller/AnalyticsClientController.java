package org.collapseloader.atlas.domain.analytics.controller;

import jakarta.validation.Valid;
import org.collapseloader.atlas.domain.analytics.dto.request.AnalyticsClientRecordRequest;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsClientService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/clients")
public class AnalyticsClientController {
    private final AnalyticsClientService analyticsClientService;

    public AnalyticsClientController(AnalyticsClientService analyticsClientService) {
        this.analyticsClientService = analyticsClientService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> recordClientLaunch(@Valid @RequestBody AnalyticsClientRecordRequest request) {
        try {
            analyticsClientService.recordClientLaunch(request.clientName(), request.platform());
            return ResponseEntity.ok(ApiResponse.success("Client launch recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error("Failed to record client launch"));
        }
    }
}
