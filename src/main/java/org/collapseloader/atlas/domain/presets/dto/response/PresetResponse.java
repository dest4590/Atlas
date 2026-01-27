package org.collapseloader.atlas.domain.presets.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PresetResponse(
        Long id,
        String name,
        String title,
        String description,
        @JsonProperty("is_public")
        boolean isPublic,
        @JsonProperty("likes_count")
        long likes,
        @JsonProperty("downloads_count")
        long downloads,
        @JsonProperty("comments_count")
        long comments,
        PresetThemeResponse theme,
        PresetAuthorResponse author,
        boolean liked,
        Instant createdAt,
        Instant updatedAt
) {
}
