package org.collapseloader.atlas.domain.analytics.dto.response;

public record GrafanaClientLaunchPointResponse(
        long time,
        String client,
        long launches) {
}
