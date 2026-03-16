package org.collapseloader.atlas.domain.users.dto.request;

import jakarta.validation.constraints.Max;

public record UserSearchParams(
        String q,

        @Max(value = 30, message = "limit must be at most 30")
        int limit
) {
}
