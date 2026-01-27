package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.entity.Role;

import java.time.Instant;

public record UserMeResponse(
        Long id,
        String username,
        String email,
        Role role,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("last_login_at") Instant lastLoginAt,
        UserProfileResponse profile,
        UserStatusResponse status
) {
}
