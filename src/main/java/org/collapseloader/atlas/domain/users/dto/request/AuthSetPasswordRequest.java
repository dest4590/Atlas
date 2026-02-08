package org.collapseloader.atlas.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthSetPasswordRequest(
        @NotBlank(message = "newPassword is required") @Size(min = 8, message = "newPassword must be at least 8 characters") String newPassword,
        @NotBlank(message = "currentPassword is required") String currentPassword) {
}
