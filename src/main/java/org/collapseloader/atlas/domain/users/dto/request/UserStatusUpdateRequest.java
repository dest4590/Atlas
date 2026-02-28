package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.users.entity.UserStatus;

public record UserStatusUpdateRequest(
        @NotNull(message = "status is required")
        UserStatus status,
        @JsonProperty("client_name")
        @Size(max = 120, message = "client_name must be at most 120 characters")
        String clientName
) {
}
