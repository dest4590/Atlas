package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicUserResponse(
        Long id,
        String username,
        UserProfileResponse profile,
        UserStatusResponse status,
        @JsonProperty("friendship_status") String friendshipStatus
) {
}
