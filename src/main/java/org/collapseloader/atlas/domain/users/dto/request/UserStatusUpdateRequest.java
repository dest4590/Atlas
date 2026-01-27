package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.entity.UserStatus;

public record UserStatusUpdateRequest(
        UserStatus status,
        @JsonProperty("client_name") String clientName
) {
}
