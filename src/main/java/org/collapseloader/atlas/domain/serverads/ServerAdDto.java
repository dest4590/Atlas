package org.collapseloader.atlas.domain.serverads;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.time.Instant;

@Value
public class ServerAdDto {
    Long id;
    String name;
    String ip;
    boolean active;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;
}
