package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record ClientRatingResponse(
        @JsonProperty("rating_avg") Double ratingAvg,
        @JsonProperty("rating_count") Integer ratingCount,
        @JsonProperty("my_rating") Integer myRating
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
