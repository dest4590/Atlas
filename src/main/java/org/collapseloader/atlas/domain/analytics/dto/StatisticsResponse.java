package org.collapseloader.atlas.domain.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatisticsResponse(
        @JsonProperty("total_client_launches") long totalClientLaunches,
        @JsonProperty("total_client_downloads") long totalClientDownloads,
        @JsonProperty("total_loader_launches") long totalLoaderLaunches
) {
}
