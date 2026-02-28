package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

public record UserExternalAccountRequest(
        @JsonProperty("display_name")
        @Size(max = 100, message = "display_name must be at most 100 characters")
        String displayName,
        JsonNode metadata
) {
}
