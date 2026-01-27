package org.collapseloader.atlas.domain.friends.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record FriendRequestResponse(
        Long id,
        FriendResponse requester,
        FriendResponse addressee,
        String status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
