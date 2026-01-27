package org.collapseloader.atlas.domain.friends.dto.response;

import org.collapseloader.atlas.domain.users.dto.response.UserStatusResponse;

public record FriendResponse(
        Long id,
        String username,
        String nickname,
        UserStatusResponse status
) {
}
