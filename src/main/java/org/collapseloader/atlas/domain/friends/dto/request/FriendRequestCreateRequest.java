package org.collapseloader.atlas.domain.friends.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FriendRequestCreateRequest(
        @JsonProperty("user_id")
        @NotNull(message = "user_id is required")
        @Positive(message = "user_id must be positive")
        Long userId
) {
}
