package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        String version,
        String filename,
        @JsonProperty("md5_hash") String md5Hash,
        Long size,
        @JsonProperty("main_class") String mainClass,
        boolean show,
        boolean working,
        long launches,
        long downloads,
        @JsonProperty("client_type") String clientType,
        @JsonProperty("created_at") Instant createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
