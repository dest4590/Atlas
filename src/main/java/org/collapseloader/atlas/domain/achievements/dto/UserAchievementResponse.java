package org.collapseloader.atlas.domain.achievements.dto;

import java.time.Instant;

public record UserAchievementResponse(
        AchievementResponse achievement,
        Instant unlockedAt) {
}
