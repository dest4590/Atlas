package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.domain.analytics.dto.StatisticsResponse;
import org.collapseloader.atlas.domain.analytics.entity.OnlineUserSnapshot;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/analytics/online/history")
    public ResponseEntity<ApiResponse<List<OnlineUserSnapshot>>> getOnlineHistory(
            @RequestParam(defaultValue = "24") int hours) {
        List<OnlineUserSnapshot> history = analyticsService.getOnlineHistory(hours);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
