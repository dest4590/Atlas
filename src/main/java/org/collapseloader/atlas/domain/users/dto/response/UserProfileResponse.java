package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String nickname,
        @JsonProperty("avatar_path") String avatarPath,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("avatar_updated_at") Instant avatarUpdatedAt,
        ProfileRole role,
        @JsonProperty("social_links") List<SocialLinkResponse> socialLinks,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
