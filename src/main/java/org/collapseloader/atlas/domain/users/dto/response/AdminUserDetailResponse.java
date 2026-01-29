package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

import java.util.List;

public record AdminUserDetailResponse(
        Long id,
        String username,
        String email,
        String role,
        @JsonProperty("enabled") boolean enabled,
        UserProfileDto profile,
        List<UserPreferenceDto> preferences) {
    public record UserProfileDto(
            String nickname,
            String avatarPath,
            ProfileRole role,
            List<SocialLinkDto> socialLinks) {
    }

    public record SocialLinkDto(
            Long id,
            SocialPlatform platform,
            String url) {
    }

    public record UserPreferenceDto(
            Long id,
            String key,
            @JsonRawValue String value) {
    }
}
