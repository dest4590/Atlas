package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50, message = "nickname must be at most 50 characters") String nickname,
        @JsonProperty("favorite_client_id") @Positive(message = "favorite_client_id must be positive") Long favoriteClientId) {
}
