package org.collapseloader.atlas.domain.achievements.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.achievements.dto.AchievementResponse;
import org.collapseloader.atlas.domain.achievements.dto.UserAchievementResponse;
import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserAchievementResponse>> getUserAchievements(@PathVariable Long userId) {
        return ResponseEntity.ok(achievementService.getUserAchievements(userId));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        achievementService.seedAchievements();
    }
}
