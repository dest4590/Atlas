package org.collapseloader.atlas.domain.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.analytics.dto.response.AdminAnalyticsClientRecordResponse;
import org.collapseloader.atlas.domain.analytics.dto.response.AdminAnalyticsServerRecordResponse;
import org.collapseloader.atlas.domain.analytics.dto.response.GrafanaClientLaunchPointResponse;
import org.collapseloader.atlas.domain.analytics.dto.response.GrafanaServerJoinPointResponse;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsClientService;
import org.collapseloader.atlas.domain.analytics.service.AnalyticsServerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsServerService analyticsServerService;
    private final AnalyticsClientService analyticsClientService;

    @GetMapping("/servers")
    public List<AdminAnalyticsServerRecordResponse> getServerRecords() {
        return analyticsServerService.getServerRecords();
    }

    @GetMapping("/clients")
    public List<AdminAnalyticsClientRecordResponse> getClientRecords(
            @RequestParam(defaultValue = "200") int limit) {
        return analyticsClientService.getRecentClientRecords(limit);
    }

    @GetMapping("/grafana/servers")
    public List<GrafanaServerJoinPointResponse> getGrafanaServerSeries() {
        return analyticsServerService.getGrafanaServerSeries();
    }

    @GetMapping("/grafana/client-launches")
    public List<GrafanaClientLaunchPointResponse> getGrafanaClientSeries(
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(defaultValue = "60") Integer intervalMinutes) {
        return analyticsClientService.getGrafanaLaunchSeries(from, to, intervalMinutes);
    }
}
