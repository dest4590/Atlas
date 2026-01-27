package org.collapseloader.atlas.domain.users.dto.request;

import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

import java.util.List;

public record AdminUserUpdateRequest(
        String username,
        boolean enabled,
        String role,
        String nickname,
        String avatarPath,
        ProfileRole profileRole,
        List<SocialLinkRequest> socialLinks,
        List<UserPreferenceRequest> preferences
) {
    public record SocialLinkRequest(
            Long id,
            SocialPlatform platform,
            String url
    ) {
    }

    public record UserPreferenceRequest(
            String key,
            Object value
    ) {
    }
}
