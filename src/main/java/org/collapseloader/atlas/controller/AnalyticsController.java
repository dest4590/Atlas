package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.domain.analytics.dto.StatisticsResponse;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/loader/launch")
    public ResponseEntity<ApiResponse<Map<String, Long>>> trackLoaderLaunch() {
        long total = analyticsService.incrementLoaderLaunches();
        return ResponseEntity.ok(ApiResponse.success(Map.of("total_loader_launches", total)));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(analyticsService.getStatistics());
    }
}
