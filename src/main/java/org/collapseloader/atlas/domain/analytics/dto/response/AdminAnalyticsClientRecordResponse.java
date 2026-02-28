package org.collapseloader.atlas.domain.analytics.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminAnalyticsClientRecordResponse(
        Long id,
        @JsonProperty("client_id") Long clientId,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("launch_timestamp") Long launchTimestamp) {
}
