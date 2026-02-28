package org.collapseloader.atlas.domain.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyticsServerRecordRequest(
        @NotBlank(message = "server is required")
        @Size(max = 255, message = "server must be at most 255 characters")
        String server) {

}
