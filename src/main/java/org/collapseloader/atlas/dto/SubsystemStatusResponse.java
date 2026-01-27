package org.collapseloader.atlas.dto;

import java.util.Map;

public record SubsystemStatusResponse(
        String status,
        String detail,
        Map<String, Object> info
) {
}
