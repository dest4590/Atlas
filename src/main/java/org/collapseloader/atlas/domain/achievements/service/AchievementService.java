package org.collapseloader.atlas.domain.achievements.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.achievements.dto.AchievementResponse;
import org.collapseloader.atlas.domain.achievements.dto.UserAchievementResponse;
import org.collapseloader.atlas.domain.achievements.entity.Achievement;
import org.collapseloader.atlas.domain.achievements.entity.UserAchievement;
import org.collapseloader.atlas.domain.achievements.repository.AchievementRepository;
import org.collapseloader.atlas.domain.achievements.repository.UserAchievementRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievements() {
        return achievementRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserAchievementResponse> getUserAchievements(Long userId) {
        return userAchievementRepository.findAllWithAchievementByUserId(userId).stream()
                .map(ua -> new UserAchievementResponse(mapToResponse(ua.getAchievement()), ua.getUnlockedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void unlockAchievement(Long userId, String key) {
        if (userAchievementRepository.existsByUserIdAndAchievementKey(userId, key)) {
            return;
        }

        var achievement = achievementRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + key));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserAchievement ua = new UserAchievement(user, achievement);
        userAchievementRepository.save(ua);
        log.info("Unlocked achievement {} for user {}", key, userId);
    }

    @Transactional
    public void seedAchievements() {
        createIfNotExists("WELCOME", "PartyPopper", false);
        createIfNotExists("PLAYED_1Hour", "Clock", false);
        createIfNotExists("PLAYED_10Hours", "Clock", false);
        createIfNotExists("FIRST_GAME", "Gamepad2", false);
        createIfNotExists("SECRET_FINDER", "Eye", true);
        createIfNotExists("SOCIAL_BUTTERFLY", "Share2", false);
        createIfNotExists("FREQUENT_FLYER", "Zap", false);
        createIfNotExists("PRESET_MAX", "Palette", false);
        createIfNotExists("BETA_TESTER", "FlaskConical", false);
        createIfNotExists("COLLECTOR", "Library", false);
    }

    private void createIfNotExists(String key, String icon, boolean hidden) {
        if (achievementRepository.findByKey(key).isEmpty()) {
            achievementRepository.save(new Achievement(key, icon, hidden));
        }
    }

    private AchievementResponse mapToResponse(Achievement achievement) {
        return new AchievementResponse(
                achievement.getId(),
                achievement.getKey(),
                achievement.getIcon(),
                achievement.isHidden());
    }
}
