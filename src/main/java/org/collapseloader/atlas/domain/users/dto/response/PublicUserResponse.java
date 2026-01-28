package org.collapseloader.atlas.domain.users.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.collapseloader.atlas.domain.achievements.dto.UserAchievementResponse;
import org.collapseloader.atlas.domain.presets.dto.response.PresetResponse;

import java.util.List;

public record PublicUserResponse(
        Long id,
        String username,
        UserProfileResponse profile,
        UserStatusResponse status,
        @JsonProperty("friendship_status") String friendshipStatus,
        List<UserAchievementResponse> achievements,
        List<PresetResponse> presets) {
}
