package org.collapseloader.atlas.domain.clients.dto.request;

public record FabricDependenceRequest(
        String name,
        String md5Hash,
        long size
) {
}
