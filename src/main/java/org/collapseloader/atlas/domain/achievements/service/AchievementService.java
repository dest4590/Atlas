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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievements() {
        long totalUsers = userRepository.count();

        if (totalUsers == 0)
            totalUsers = 1;

        List<Achievement> achievements = achievementRepository.findAll();
        long finalTotalUsers = totalUsers;

        return achievements.stream()
                .map(a -> {
                    long count = userAchievementRepository.countByAchievementId(a.getId());
                    double percentage = (double) count / finalTotalUsers * 100.0;
                    percentage = Math.round(percentage * 10.0) / 10.0;
                    return mapToResponse(a, percentage);
                })
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
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/achievements",
                Map.of("key", key));
    }

    @Transactional
    public void revokeAchievement(Long userId, String key) {
        var achievement = achievementRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + key));

        var ua = userAchievementRepository.findByUserIdAndAchievementId(userId, achievement.getId())
                .orElseThrow(() -> new IllegalArgumentException("User does not have achievement: " + key));

        userAchievementRepository.delete(ua);
        log.info("Revoked achievement {} from user {}", key, userId);
    }

    @Transactional
    public void createAchievement(String key, String icon, boolean hidden) {
        if (achievementRepository.findByKey(key).isPresent()) {
            throw new IllegalArgumentException("Achievement already exists: " + key);
        }
        achievementRepository.save(new Achievement(key, icon, hidden));
    }

    @Transactional
    public void updateAchievement(Long id, String key, String icon, Boolean hidden) {
        var achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + id));

        if (key != null && !key.equals(achievement.getKey())) {
            if (achievementRepository.findByKey(key).isPresent()) {
                throw new IllegalArgumentException("Achievement key already exists: " + key);
            }
            achievement.setKey(key);
        }

        if (icon != null)
            achievement.setIcon(icon);
        if (hidden != null)
            achievement.setHidden(hidden);

        achievementRepository.save(achievement);
    }

    @Transactional
    public void deleteAchievement(Long id) {
        if (!achievementRepository.existsById(id)) {
            throw new IllegalArgumentException("Achievement not found: " + id);
        }

        if (userAchievementRepository.countByAchievementId(id) > 0) {
            var links = userAchievementRepository.findAll().stream()
                    .filter(ua -> ua.getAchievement().getId().equals(id)).collect(Collectors.toList());
            userAchievementRepository.deleteAll(links);
        }

        achievementRepository.deleteById(id);
    }

    @Transactional
    public void seedAchievements() {
        createIfNotExists("PLAYED_1Hour", "Clock", false);
        createIfNotExists("PLAYED_10Hours", "Clock", false);
        createIfNotExists("FIRST_GAME", "Gamepad2", false);
        createIfNotExists("SECRET_FINDER", "Eye", true);
        createIfNotExists("SOCIAL_BUTTERFLY", "Share2", false);
        createIfNotExists("FREQUENT_FLYER", "Zap", false);
        createIfNotExists("PRESET_MAX", "Palette", false);
        createIfNotExists("BETA_TESTER", "FlaskConical", false);
        createIfNotExists("COLLECTOR", "Library", false);
        createIfNotExists("NIGHT_OWL", "Moon", false);
        createIfNotExists("EARLY_BIRD", "Sun", false);
        createIfNotExists("WEEKEND_WARRIOR", "Calendar", false);
    }

    private void createIfNotExists(String key, String icon, boolean hidden) {
        if (achievementRepository.findByKey(key).isEmpty()) {
            achievementRepository.save(new Achievement(key, icon, hidden));
        }
    }

    private AchievementResponse mapToResponse(Achievement achievement, Double percentage) {
        return new AchievementResponse(
                achievement.getId(),
                achievement.getKey(),
                achievement.getIcon(),
                achievement.isHidden(),
                percentage);
    }

    private AchievementResponse mapToResponse(Achievement achievement) {
        long totalUsers = userRepository.count();
        if (totalUsers == 0)
            totalUsers = 1;
        long count = userAchievementRepository.countByAchievementId(achievement.getId());
        double percentage = (double) count / totalUsers * 100.0;
        percentage = Math.round(percentage * 10.0) / 10.0;
        return mapToResponse(achievement, percentage);
    }
}
