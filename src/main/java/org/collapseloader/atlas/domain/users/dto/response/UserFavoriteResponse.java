package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UserFavoriteResponse(
        Long id,
        String type,
        String reference,
        Object metadata,
        @JsonProperty("created_at") Instant createdAt
) {
}
