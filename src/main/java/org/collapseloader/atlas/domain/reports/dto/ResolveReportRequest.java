package org.collapseloader.atlas.domain.reports.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.collapseloader.atlas.domain.reports.entity.ReportStatus;

@Data
public class ResolveReportRequest {
    @NotNull(message = "Status is required")
    private ReportStatus status;
    private String adminNotes;
}
