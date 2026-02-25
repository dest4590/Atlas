package org.collapseloader.atlas.domain.crashlogs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrashLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String clientName;
    private String clientVersion;
    private String crashType;
    private String loaderVersion;
    private String osName;
    private String osVersion;
    private Integer lineCount;
    private String logContent;
    private Instant createdAt;
}
