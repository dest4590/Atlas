package org.collapseloader.atlas.domain.crashlogs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCrashLogRequest {
    @NotBlank(message = "Client name is required")
    private String clientName;

    private String clientVersion;

    @NotBlank(message = "Crash type is required")
    private String crashType;

    private String loaderVersion;
    private String osName;
    private String osVersion;

    private Integer lineCount;

    @NotBlank(message = "Log content is required")
    private String logContent;
}
