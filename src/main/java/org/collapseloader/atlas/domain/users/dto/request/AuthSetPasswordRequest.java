package org.collapseloader.atlas.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthSetPasswordRequest(
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 128, message = "newPassword must be between 8 and 128 characters")
        String newPassword,
        @NotBlank(message = "currentPassword is required")
        @Size(min = 8, max = 128, message = "currentPassword must be between 8 and 128 characters")
        String currentPassword) {
}
