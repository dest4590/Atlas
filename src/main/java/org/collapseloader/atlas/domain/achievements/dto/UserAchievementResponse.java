package org.collapseloader.atlas.domain.achievements.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementResponse {
    private AchievementResponse achievement;
    private Instant unlockedAt;
}
