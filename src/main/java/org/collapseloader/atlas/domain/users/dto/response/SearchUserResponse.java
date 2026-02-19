package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchUserResponse(
        Long id,
        String username,
        String nickname,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("friendship_status") String friendshipStatus
) {
}
