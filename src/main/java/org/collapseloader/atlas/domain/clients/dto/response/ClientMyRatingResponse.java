package org.collapseloader.atlas.domain.clients.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record ClientMyRatingResponse(
        @JsonProperty("my_rating") Double myRating) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
