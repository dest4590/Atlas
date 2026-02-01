package org.collapseloader.atlas.domain.users.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UserStatusCleanupService {
    private static final Logger log = LoggerFactory.getLogger(UserStatusCleanupService.class);
    private final UserStatusService userStatusService;

    public UserStatusCleanupService(UserStatusService userStatusService) {
        this.userStatusService = userStatusService;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupStaleStatuses() {
        try {
            userStatusService.processStaleStatus();
        } catch (Exception e) {
            log.error("Failed to cleanup stale user statuses", e);
        }
    }
}
