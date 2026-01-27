package org.collapseloader.atlas.domain.achievements.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementResponse {
    private Long id;
    private String key;
    private String icon;
    private boolean hidden;
}
