package org.collapseloader.atlas.domain.presets.dto.response;

public record PresetAuthorResponse(
        Long id,
        String username,
        String nickname,
        String avatar
) {
}
