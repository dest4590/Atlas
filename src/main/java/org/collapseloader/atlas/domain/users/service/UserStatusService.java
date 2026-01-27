package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.users.dto.response.UserStatusResponse;
import org.collapseloader.atlas.domain.users.entity.UserStatus;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserStatusService {
    private static final String KEY_PREFIX = "user:status:";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CLIENT_NAME = "clientName";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final StringRedisTemplate redisTemplate;

    public UserStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public UserStatusResponse getStatus(Long userId) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> data = ops.entries(key(userId));
        if (data == null || data.isEmpty()) {
            return new UserStatusResponse(UserStatus.OFFLINE, null, null);
        }
        return new UserStatusResponse(
                parseStatus(data.get(FIELD_STATUS)),
                parseClientName(data.get(FIELD_CLIENT_NAME)),
                parseUpdatedAt(data.get(FIELD_UPDATED_AT))
        );
    }

    public UserStatusResponse setStatus(Long userId, UserStatus status, String clientName) {
        if (status == null) {
            throw new RuntimeException("Status is required");
        }
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = key(userId);

        Map<String, String> updates = new HashMap<>();
        updates.put(FIELD_STATUS, status.name());
        updates.put(FIELD_UPDATED_AT, String.valueOf(Instant.now().toEpochMilli()));
        ops.putAll(key, updates);

        if (status == UserStatus.ONLINE && clientName != null && !clientName.isBlank()) {
            ops.put(key, FIELD_CLIENT_NAME, clientName);
        } else {
            ops.delete(key, FIELD_CLIENT_NAME);
        }

        return getStatus(userId);
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
