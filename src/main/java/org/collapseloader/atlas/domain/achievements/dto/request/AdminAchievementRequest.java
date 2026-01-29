package org.collapseloader.atlas.domain.achievements.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminAchievementRequest(String key, String icon, @JsonProperty("hidden") Boolean hidden) {
}
