package org.collapseloader.atlas.domain.crashlogs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.crashlogs.dto.CrashLogResponse;
import org.collapseloader.atlas.domain.crashlogs.dto.CreateCrashLogRequest;
import org.collapseloader.atlas.domain.crashlogs.entity.CrashLog;
import org.collapseloader.atlas.domain.crashlogs.repository.CrashLogRepository;
import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrashLogService {
    private final CrashLogRepository crashLogRepository;

    @Value("${atlas.crash-logs.max-log-chars:40000}")
    private int maxLogChars;

    @Value("${atlas.crash-logs.retention-days:30}")
    private int retentionDays;

    @Transactional
    public CrashLogResponse createCrashLog(User user, CreateCrashLogRequest request) {
        String sanitizedLog = maskAndTrim(request.getLogContent());
        String usernameSnapshot = user != null ? user.getUsername() : "guest";

        CrashLog crashLog = CrashLog.builder()
                .user(user)
                .usernameSnapshot(usernameSnapshot)
                .clientName(sanitizeField(request.getClientName(), 120))
                .clientVersion(sanitizeField(request.getClientVersion(), 64))
                .crashType(sanitizeField(request.getCrashType(), 120))
                .loaderVersion(sanitizeField(request.getLoaderVersion(), 64))
                .osName(sanitizeField(request.getOsName(), 64))
                .osVersion(sanitizeField(request.getOsVersion(), 64))
                .lineCount(request.getLineCount() == null ? null : Math.max(request.getLineCount(), 0))
                .logContent(sanitizedLog)
                .build();

        return mapToResponse(crashLogRepository.save(crashLog));
    }

    @Transactional(readOnly = true)
    public Page<CrashLogResponse> getCrashLogs(Pageable pageable) {
        return crashLogRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    @Scheduled(cron = "${atlas.crash-logs.cleanup-cron:0 0 */6 * * *}")
    public void cleanupOldCrashLogs() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = crashLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} old crash logs (older than {} days)", deleted, retentionDays);
        }
    }

    private CrashLogResponse mapToResponse(CrashLog crashLog) {
        Long userId = crashLog.getUser() != null ? crashLog.getUser().getId() : null;
        return CrashLogResponse.builder()
                .id(crashLog.getId())
                .userId(userId)
                .username(crashLog.getUsernameSnapshot())
                .clientName(crashLog.getClientName())
                .clientVersion(crashLog.getClientVersion())
                .crashType(crashLog.getCrashType())
                .loaderVersion(crashLog.getLoaderVersion())
                .osName(crashLog.getOsName())
                .osVersion(crashLog.getOsVersion())
                .lineCount(crashLog.getLineCount())
                .logContent(crashLog.getLogContent())
                .createdAt(crashLog.getCreatedAt())
                .build();
    }

    private String maskAndTrim(String input) {
        String safe = input == null ? "" : input.replace("\u0000", "");

        if (safe.length() <= maxLogChars) {
            return safe;
        }

        int headSize = maxLogChars / 2;
        int tailSize = maxLogChars - headSize;
        String head = safe.substring(0, headSize);
        String tail = safe.substring(safe.length() - tailSize);
        return head + "\n...[TRIMMED MIDDLE]...\n" + tail;
    }

    private String sanitizeField(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("\u0000", "").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }

}
