package org.collapseloader.atlas.domain.clients.dto.request;

import org.collapseloader.atlas.domain.clients.entity.ClientType;

public record AdminClientRequest(
        String name,
        String version,
        String filename,
        String md5Hash,
        Long size,
        String mainClass,
        Boolean show,
        Boolean working,
        Long launches,
        Long downloads,
        ClientType type
) {
}
