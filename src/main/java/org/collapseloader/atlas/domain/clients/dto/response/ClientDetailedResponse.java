package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record ClientDetailedResponse(
        Long id,
        String name,
        String version,
        @JsonProperty("source_link") String sourceLink,
        @JsonProperty("screenshot_urls") List<String> screenshotUrls,
        @JsonProperty("rating_avg") Double ratingAvg,
        @JsonProperty("rating_count") Integer ratingCount,
        @JsonProperty("comments_count") Integer commentsCount,
        @JsonProperty("created_at") LocalDateTime createdAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
