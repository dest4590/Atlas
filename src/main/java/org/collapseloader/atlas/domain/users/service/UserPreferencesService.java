package org.collapseloader.atlas.domain.users.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.collapseloader.atlas.domain.users.dto.response.UserPreferenceResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.entity.UserPreference;
import org.collapseloader.atlas.domain.users.repository.UserPreferenceRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.collapseloader.atlas.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserPreferencesService {
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    public UserPreferencesService(
            UserRepository userRepository,
            UserPreferenceRepository userPreferenceRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> getPreferences(User principal) {
        var userId = principal.getId();
        return userPreferenceRepository.findByUserId(userId).stream()
                .map(this::mapPreference)
                .toList();
    }

    @Transactional
    public UserPreferenceResponse upsertPreference(User principal, String key, Object value) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            throw new ValidationException("Preference key is required");
        }
        if (value == null) {
            throw new ValidationException("Preference value is required");
        }
        JsonNode node = objectMapper.valueToTree(value);
        if (node == null || node.isNull()) {
            throw new ValidationException("Preference value is required");
        }

        if (node.isObject() && node.has("value") && node.size() == 1) {
            node = node.get("value");
        }

        var preference = userPreferenceRepository.findByUserIdAndKey(user.getId(), normalizedKey)
                .orElseGet(() -> UserPreference.builder()
                        .user(user)
                        .key(normalizedKey)
                        .build());

        if (node.isObject() && node.has("value")) {
            node = node.get("value");
        }

        preference.setValue(node);

        return mapPreference(userPreferenceRepository.save(preference));
    }

    @Transactional
    public void deletePreference(User principal, String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return;
        }
        userPreferenceRepository.deleteByUserIdAndKey(principal.getId(), normalizedKey);
    }

    private UserPreferenceResponse mapPreference(UserPreference preference) {
        return new UserPreferenceResponse(
                preference.getKey(),
                normalizeValue(preference.getValue()),
                preference.getCreatedAt(),
                preference.getUpdatedAt());
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private Object normalizeValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, Object.class);
    }
}
