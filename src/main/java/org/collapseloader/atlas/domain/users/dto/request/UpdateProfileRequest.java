package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateProfileRequest(
        String nickname,
        @JsonProperty("favorite_client_id") Long favoriteClientId) {
}
