package org.collapseloader.atlas.domain.users.dto.response;

import java.time.Instant;

public record UserAdminResponse(
        Long id,
        String username,
        String email,
        String role,
        String profileRole,
        boolean enabled,
        Instant createdAt) {
}
