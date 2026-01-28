package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.users.entity.UserStatus;

import java.time.Instant;

public record UserStatusResponse(
        UserStatus status,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("started_at") Instant startedAt) {
}
