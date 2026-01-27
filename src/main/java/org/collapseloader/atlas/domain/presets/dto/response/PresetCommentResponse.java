package org.collapseloader.atlas.domain.presets.dto.response;

import java.time.Instant;

public record PresetCommentResponse(
        Long id,
        Long presetId,
        Long authorId,
        String authorUsername,
        String authorNickname,
        String avatar,
        String text,
        Instant createdAt
) {
}
