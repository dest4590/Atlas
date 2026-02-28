package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserFavoriteRequest(
        @NotBlank(message = "type is required")
        @Size(max = 50, message = "type must be at most 50 characters")
        String type,
        @NotBlank(message = "reference is required")
        @Size(max = 255, message = "reference must be at most 255 characters")
        String reference,
        JsonNode metadata) {
}
