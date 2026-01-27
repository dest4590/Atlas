package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record UserExternalAccountRequest(
        String provider,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("display_name") String displayName,
        JsonNode metadata
) {
}
