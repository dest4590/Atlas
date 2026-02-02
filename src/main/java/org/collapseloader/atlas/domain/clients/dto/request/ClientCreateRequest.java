package org.collapseloader.atlas.domain.clients.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.Version;

public record ClientCreateRequest(
        String name,
        Version version,
        String filename,
        @JsonProperty("md5_hash") String md5Hash,
        Long size,
        @JsonProperty("main_class") String mainClass,
        Boolean show,
        Boolean working,
        Long launches,
        Long downloads,
        @JsonProperty("client_type") ClientType clientType
) {
    public static ClientCreateRequest fromAdmin(AdminClientRequest request) {
        if (request == null) {
            return new ClientCreateRequest(null, null, null, null, null, null, null, null, null, null, null);
        }
        return new ClientCreateRequest(
                request.name(),
                request.version(),
                request.filename(),
                request.md5Hash(),
                request.size(),
                request.mainClass(),
                request.show(),
                request.working(),
                request.launches(),
                request.downloads(),
                request.type());
    }
}
