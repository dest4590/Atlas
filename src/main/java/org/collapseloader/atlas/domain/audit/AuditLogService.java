package org.collapseloader.atlas.domain.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private static final org.slf4j.Logger AUDIT_LOGGER = org.slf4j.LoggerFactory.getLogger("AUDIT");
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String targetType, String targetId, String adminUsername, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .adminUsername(adminUsername)
                .details(details)
                .build();
        auditLogRepository.save(log);

        AUDIT_LOGGER.info("[{}] {} performed {} on {}:{} - {}",
                log.getCreatedAt(), adminUsername, action, targetType, targetId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
