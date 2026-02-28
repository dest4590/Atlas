package org.collapseloader.atlas.domain.analytics.dto.response;

public record GrafanaServerJoinPointResponse(
        long time,
        String domain,
        long joins) {
}
