package org.collapseloader.atlas.domain.analytics.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminAnalyticsServerRecordResponse(
        Long id,
        String domain,
        @JsonProperty("join_count") Long joinCount) {
}
