package org.collapseloader.atlas.domain.status.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record ServerStatusResponse(
        String project,
        String status,
        String version,
        String environment,
        Instant timestamp,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("uptime_seconds") long uptimeSeconds,
        Map<String, SubsystemStatusResponse> checks
) {
}
