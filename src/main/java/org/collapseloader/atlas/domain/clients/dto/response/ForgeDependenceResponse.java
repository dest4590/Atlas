package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record ForgeDependenceResponse(
        String name,
        @JsonProperty("md5_hash") String md5Hash,
        long size
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
