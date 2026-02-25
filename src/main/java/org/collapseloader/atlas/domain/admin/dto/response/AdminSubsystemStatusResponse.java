package org.collapseloader.atlas.domain.admin.dto.response;

import java.util.Map;

public record AdminSubsystemStatusResponse(
        String status,
        String detail,
        Map<String, Object> info
) {
}
