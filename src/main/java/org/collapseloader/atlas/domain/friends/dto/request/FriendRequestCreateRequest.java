package org.collapseloader.atlas.domain.friends.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FriendRequestCreateRequest(
        @JsonProperty("user_id") Long userId
) {
}
