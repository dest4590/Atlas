package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record UserFavoriteResponse(
        Long id,
        String type,
        String reference,
        JsonNode metadata,
        @JsonProperty("created_at") Instant createdAt
) {
}
