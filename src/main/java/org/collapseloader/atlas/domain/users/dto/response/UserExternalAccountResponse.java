package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserExternalAccountResponse(
        Long id,
        String provider,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("display_name") String displayName,
        Object metadata,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
