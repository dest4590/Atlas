package org.collapseloader.atlas.domain.admin.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.achievements.dto.AchievementResponse;
import org.collapseloader.atlas.domain.achievements.dto.request.AdminAchievementRequest;
import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/achievements")
@RequiredArgsConstructor
public class AdminAchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AchievementResponse>> getAll() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> create(@RequestBody AdminAchievementRequest request) {
        achievementService.createAchievement(request.key(), request.icon(),
                request.hidden() != null && request.hidden());
        return ResponseEntity.created(URI.create("/api/v1/admin/achievements/" + request.key())).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody AdminAchievementRequest request) {
        achievementService.updateAchievement(id, request.key(), request.icon(), request.hidden());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }
}
