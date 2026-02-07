package org.collapseloader.atlas.domain.clients.dto.request;

public record ForgeDependenceRequest(
        String name,
        String md5Hash,
        long size
) {
}
