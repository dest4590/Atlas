package org.collapseloader.atlas.domain.clients.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.clients.entity.ClientType;

public record AdminClientRequest(
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
        @Size(max = 64, message = "version must be at most 64 characters")
        String version,
        @Size(max = 255, message = "filename must be at most 255 characters")
        String filename,
        @Pattern(regexp = "^[A-Fa-f0-9]{32}$", message = "md5Hash must be a valid 32-character hex string")
        String md5Hash,
        @PositiveOrZero(message = "size must be greater than or equal to 0")
        Long size,
        @Size(max = 255, message = "mainClass must be at most 255 characters")
        String mainClass,
        Boolean show,
        Boolean working,
        @PositiveOrZero(message = "launches must be greater than or equal to 0")
        Long launches,
        @PositiveOrZero(message = "downloads must be greater than or equal to 0")
        Long downloads,
        ClientType type
) {
}
