package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record UserExternalAccountRequest(
        @JsonProperty("display_name") String displayName,
        JsonNode metadata
) {
}
