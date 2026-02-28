package org.collapseloader.atlas.domain.achievements.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAchievementRequest(
        @Size(max = 64, message = "key must be at most 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "key must contain only letters, numbers, and underscores")
        String key,
        @Size(max = 128, message = "icon must be at most 128 characters")
        String icon,
        @JsonProperty("hidden") Boolean hidden) {
}
