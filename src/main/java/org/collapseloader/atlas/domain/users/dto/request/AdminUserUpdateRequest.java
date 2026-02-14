package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

import java.util.List;

public record AdminUserUpdateRequest(
                String username,
                @JsonProperty("enabled") boolean enabled,
                String role,
                String password,
                String nickname,
                String avatarPath,
                ProfileRole profileRole,
                List<SocialLinkRequest> socialLinks,
                List<UserPreferenceRequest> preferences) {
        public record SocialLinkRequest(
                        Long id,
                        SocialPlatform platform,
                        String url) {
        }

        public record UserPreferenceRequest(
                        String key,
                        Object value) {
        }
}
