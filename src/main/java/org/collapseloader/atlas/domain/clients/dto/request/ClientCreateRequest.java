package org.collapseloader.atlas.domain.clients.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.clients.entity.ClientType;

public record ClientCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
        @NotBlank(message = "version is required")
        @Size(max = 64, message = "version must be at most 64 characters")
        String version,
        @NotBlank(message = "filename is required")
        @Size(max = 255, message = "filename must be at most 255 characters")
        String filename,
        @JsonProperty("md5_hash")
        @NotBlank(message = "md5_hash is required")
        @Pattern(regexp = "^[A-Fa-f0-9]{32}$", message = "md5_hash must be a valid 32-character hex string")
        String md5Hash,
        @PositiveOrZero(message = "size must be greater than or equal to 0")
        Long size,
        @JsonProperty("main_class")
        @NotBlank(message = "main_class is required")
        @Size(max = 255, message = "main_class must be at most 255 characters")
        String mainClass,
        Boolean show,
        Boolean working,
        @PositiveOrZero(message = "launches must be greater than or equal to 0")
        Long launches,
        @PositiveOrZero(message = "downloads must be greater than or equal to 0")
        Long downloads,
        @JsonProperty("client_type") ClientType clientType
) {
    public static ClientCreateRequest fromAdmin(AdminClientRequest request) {
        if (request == null) {
            return new ClientCreateRequest(null, null, null, null, null, null, null, null, null, null, null);
        }
        return new ClientCreateRequest(
                request.name(),
                request.version(),
                request.filename(),
                request.md5Hash(),
                request.size(),
                request.mainClass(),
                request.show(),
                request.working(),
                request.launches(),
                request.downloads(),
                request.type());
    }
}
