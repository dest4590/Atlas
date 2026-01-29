package org.collapseloader.atlas.domain.achievements.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AchievementResponse(
        Long id,
        String key,
        String icon,
        @JsonProperty("hidden") boolean hidden,
        Double receivePercentage) {
}
