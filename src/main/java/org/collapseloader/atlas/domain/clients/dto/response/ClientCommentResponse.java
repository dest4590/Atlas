package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record ClientCommentResponse(
        Long id,
        Long client,
        Long user,
        @JsonProperty("author_username") String authorUsername,
        @JsonProperty("author_avatar") String authorAvatar,
        String content,
        @JsonProperty("created_at") LocalDateTime createdAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
