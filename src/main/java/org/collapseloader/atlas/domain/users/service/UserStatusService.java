package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.users.dto.response.UserStatusResponse;
import org.collapseloader.atlas.domain.users.entity.UserStatus;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserStatusService {
    private static final String KEY_PREFIX = "user:status:";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CLIENT_NAME = "clientName";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_STARTED_AT = "startedAt";

    private final StringRedisTemplate redisTemplate;
    private final UserProfileRepository userProfileRepository;
    private final AchievementService achievementService;
    private final SimpMessagingTemplate messagingTemplate;

    public UserStatusService(
            StringRedisTemplate redisTemplate,
            UserProfileRepository userProfileRepository,
            AchievementService achievementService,
            SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.userProfileRepository = userProfileRepository;
        this.achievementService = achievementService;
        this.messagingTemplate = messagingTemplate;
    }

    public UserStatusResponse getStatus(Long userId) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> data = ops.entries(key(userId));
        if (data == null || data.isEmpty()) {
            return new UserStatusResponse(UserStatus.OFFLINE, null, null, null);
        }
        return new UserStatusResponse(
                parseStatus(data.get(FIELD_STATUS)),
                parseClientName(data.get(FIELD_CLIENT_NAME)),
                parseUpdatedAt(data.get(FIELD_UPDATED_AT)),
                parseUpdatedAt(data.get(FIELD_STARTED_AT)));
    }

    public UserStatusResponse setStatus(Long userId, UserStatus status, String clientName) {
        if (status == null) {
            throw new RuntimeException("Status is required");
        }
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = key(userId);

        Map<String, String> currentData = ops.entries(key);
        String currentClientName = currentData.get(FIELD_CLIENT_NAME);
        UserStatus currentStatus = parseStatus(currentData.get(FIELD_STATUS));

        Map<String, String> updates = new HashMap<>();
        updates.put(FIELD_STATUS, status.name());
        updates.put(FIELD_UPDATED_AT, String.valueOf(Instant.now().toEpochMilli()));

        if (status == UserStatus.ONLINE) {
            if (clientName != null && !clientName.isBlank()) {
                updates.put(FIELD_CLIENT_NAME, clientName);
            }

            boolean isNewSession = currentStatus != UserStatus.ONLINE
                    || (clientName != null && !clientName.equals(currentClientName));

            if (isNewSession) {
                updates.put(FIELD_STARTED_AT, String.valueOf(Instant.now().toEpochMilli()));

                LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
                int hour = now.getHour();
                var dayOfWeek = now.getDayOfWeek();

                if (hour >= 2 && hour < 5) {
                    achievementService.unlockAchievement(userId, "NIGHT_OWL");
                }
                if (hour >= 5 && hour < 8) {
                    achievementService.unlockAchievement(userId, "EARLY_BIRD");
                }
                if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                    achievementService.unlockAchievement(userId, "WEEKEND_WARRIOR");
                }
            }
        } else {
            if (currentStatus == UserStatus.ONLINE) {
                String startedAtStr = currentData.get(FIELD_STARTED_AT);
                if (startedAtStr != null) {
                    try {
                        long startedAt = Long.parseLong(startedAtStr);
                        long elapsedSeconds = (Instant.now().toEpochMilli() - startedAt) / 1000;
                        if (elapsedSeconds > 0) {
                            var profile = userProfileRepository.findByUserId(userId).orElse(null);
                            if (profile != null) {
                                long total = profile.getTotalPlaytimeSeconds() + elapsedSeconds;
                                profile.setTotalPlaytimeSeconds(total);
                                userProfileRepository.save(profile);

                                if (total >= 3600) {
                                    achievementService.unlockAchievement(userId, "PLAYED_1Hour");
                                }
                                if (total >= 36000) {
                                    achievementService.unlockAchievement(userId, "PLAYED_10Hours");
                                }
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            ops.delete(key, FIELD_CLIENT_NAME);
            ops.delete(key, FIELD_STARTED_AT);
        }

        ops.putAll(key, updates);

        var response = getStatus(userId);
        messagingTemplate.convertAndSend("/topic/presence", (Object) Map.of("userId", userId, "status", response));

        return response;
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private UserStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return UserStatus.OFFLINE;
        }
        try {
            return UserStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return UserStatus.OFFLINE;
        }
    }

    private String parseClientName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private Instant parseUpdatedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
