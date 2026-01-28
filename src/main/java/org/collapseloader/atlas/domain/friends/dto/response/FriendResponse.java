package org.collapseloader.atlas.domain.friends.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.dto.response.UserStatusResponse;

public record FriendResponse(
        Long id,
        String username,
        String nickname,
        @JsonProperty("avatar_url") String avatarUrl,
        UserStatusResponse status) {
}
