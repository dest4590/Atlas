package org.collapseloader.atlas.domain.reports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotNull(message = "Reported user ID is required")
    private Long reportedUserId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;
}
