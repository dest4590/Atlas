package org.collapseloader.atlas.domain.clients.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FabricDependenceRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,
        @NotBlank(message = "md5Hash is required")
        @Pattern(regexp = "^[A-Fa-f0-9]{32}$", message = "md5Hash must be a valid 32-character hex string")
        String md5Hash,
        @PositiveOrZero(message = "size must be greater than or equal to 0")
        long size
) {
}
