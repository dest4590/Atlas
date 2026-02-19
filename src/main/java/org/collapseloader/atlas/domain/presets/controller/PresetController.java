package org.collapseloader.atlas.domain.presets.controller;

import org.collapseloader.atlas.domain.presets.dto.request.PresetCommentRequest;
import org.collapseloader.atlas.domain.presets.dto.request.PresetUpsertRequest;
import org.collapseloader.atlas.domain.presets.dto.response.PresetCommentResponse;
import org.collapseloader.atlas.domain.presets.dto.response.PresetResponse;
import org.collapseloader.atlas.domain.presets.service.PresetService;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {
    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PresetResponse>>> listPresets(
            Authentication authentication,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "owner", required = false) Long ownerId,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
        var user = optionalUser(authentication);
        var data = presetService.listPresets(user, query, ownerId, sort, limit);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PresetResponse>> getPreset(
            Authentication authentication,
            @PathVariable Long id) {
        var user = optionalUser(authentication);
        var data = presetService.getPreset(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresetResponse>> createPreset(
            Authentication authentication,
            @RequestBody PresetUpsertRequest request) {
        var user = requireUser(authentication);
        var data = presetService.createPreset(user, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresetResponse>> updatePreset(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody PresetUpsertRequest request) {
        var user = requireUser(authentication);
        var data = presetService.updatePreset(id, user, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePreset(
            Authentication authentication,
            @PathVariable Long id) {
        var user = requireUser(authentication);
        presetService.deletePreset(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresetResponse>> likePreset(
            Authentication authentication,
            @PathVariable Long id) {
        var user = requireUser(authentication);
        var data = presetService.likePreset(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{id}/unlike")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresetResponse>> unlikePreset(
            Authentication authentication,
            @PathVariable Long id) {
        var user = requireUser(authentication);
        var data = presetService.unlikePreset(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<ApiResponse<PresetResponse>> downloadPreset(
            Authentication authentication,
            @PathVariable Long id) {
        var user = optionalUser(authentication);
        var data = presetService.incrementDownloads(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<PresetCommentResponse>>> listComments(
            Authentication authentication,
            @PathVariable Long id) {
        var user = optionalUser(authentication);
        var data = presetService.listComments(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresetCommentResponse>> addComment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody PresetCommentRequest request) {
        var user = requireUser(authentication);
        var data = presetService.addComment(id, user, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{presetId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            Authentication authentication,
            @PathVariable Long presetId,
            @PathVariable Long commentId) {
        var user = requireUser(authentication);
        presetService.deleteComment(presetId, commentId, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User optionalUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private User requireUser(Authentication authentication) {
        var user = optionalUser(authentication);
        if (user == null) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
