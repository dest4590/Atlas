package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserPreferenceResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserPreferencesController {
    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<ApiResponse<List<UserPreferenceResponse>>> getPreferences(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userPreferencesService.getPreferences(user)));
    }

    @PutMapping("/me/preferences/{key:.+}")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> upsertPreference(
            Authentication authentication,
            @PathVariable String key,
            @RequestBody Object value
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userPreferencesService.upsertPreference(user, key, value)));
    }

    @DeleteMapping("/me/preferences/{key:.+}")
    public ResponseEntity<ApiResponse<Void>> deletePreference(
            Authentication authentication,
            @PathVariable String key
    ) {
        var user = requireUser(authentication);
        userPreferencesService.deletePreference(user, key);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
