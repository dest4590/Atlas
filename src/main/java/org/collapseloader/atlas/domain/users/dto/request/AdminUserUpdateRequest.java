package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

import java.util.List;

public record AdminUserUpdateRequest(
        @Size(max = 50, message = "username must be at most 50 characters")
        String username,
        @Email(message = "email must be a valid email address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,
        @JsonProperty("enabled") boolean enabled,
        @Pattern(regexp = "(?i)^(USER|ADMIN)?$", message = "role must be USER or ADMIN")
        String role,
        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password,
        @Size(max = 50, message = "nickname must be at most 50 characters")
        String nickname,
        @Size(max = 2048, message = "avatarPath must be at most 2048 characters")
        String avatarPath,
        ProfileRole profileRole,
        List<@Valid SocialLinkRequest> socialLinks,
        List<@Valid UserPreferenceRequest> preferences) {
    public record SocialLinkRequest(
            @Positive(message = "id must be positive")
            Long id,
            SocialPlatform platform,
            @Size(max = 2048, message = "url must be at most 2048 characters")
            String url) {
    }

    public record UserPreferenceRequest(
            @Size(min = 1, max = 120, message = "key must be between 1 and 120 characters")
            String key,
            Object value) {
    }
}
