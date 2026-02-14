package org.collapseloader.atlas.domain.status.dto;

import java.util.Map;

public record SubsystemStatusResponse(
        String status,
        String detail,
        Map<String, Object> info
) {
}
