package org.collapseloader.atlas.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record AdminServerStatusResponse(
        String project,
        String status,
        String version,
        String environment,
        Instant timestamp,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("uptime_seconds") long uptimeSeconds,
        Map<String, AdminSubsystemStatusResponse> checks
) {
}
