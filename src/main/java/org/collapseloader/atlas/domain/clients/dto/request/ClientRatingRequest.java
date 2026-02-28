package org.collapseloader.atlas.domain.clients.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ClientRatingRequest(
        @DecimalMin(value = "0.5", message = "rating must be at least 0.5")
        @DecimalMax(value = "5.0", message = "rating must be at most 5.0")
        double rating) {
}
