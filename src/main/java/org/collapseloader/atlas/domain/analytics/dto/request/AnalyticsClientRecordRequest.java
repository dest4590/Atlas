package org.collapseloader.atlas.domain.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.analytics.entity.Platform;

public record AnalyticsClientRecordRequest(
        @NotBlank(message = "clientName is required")
        @Size(max = 100, message = "clientName must be at most 100 characters")
        String clientName,

        @NotNull(message = "platform is required")
        Platform platform) {
}
