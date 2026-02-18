package org.collapseloader.atlas.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.collapseloader.atlas.domain.reports.entity.ReportStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportResponse {
    private Long id;
    private ReporterDto reporter;
    private ReporterDto reportedUser;
    private String reason;
    private String description;
    private ReportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private ReporterDto resolvedBy;
    private Instant resolvedAt;
    private String adminNotes;

    @Data
    @AllArgsConstructor
    public static class ReporterDto {
        private Long id;
        private String username;
    }
}
