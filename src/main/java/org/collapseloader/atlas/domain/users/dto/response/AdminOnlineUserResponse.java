package org.collapseloader.atlas.domain.users.dto.response;

import java.time.Instant;

public record AdminOnlineUserResponse(
        Long userId,
        String username,
        String avatarPath,
        String clientName,
        Instant startedAt) {
}
