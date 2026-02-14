package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.domain.users.dto.request.UserStatusUpdateRequest;
import org.collapseloader.atlas.domain.users.dto.response.UserStatusResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserStatusController {
    private final UserStatusService userStatusService;

    public UserStatusController(UserStatusService userStatusService) {
        this.userStatusService = userStatusService;
    }

    @GetMapping("/me/status")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getMyStatus(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userStatusService.getStatus(user.getId())));
    }

    @PutMapping("/me/status")
    public ResponseEntity<ApiResponse<UserStatusResponse>> updateMyStatus(
            Authentication authentication,
            @RequestBody UserStatusUpdateRequest request) {
        var user = requireUser(authentication);
        if (request == null || request.status() == null) {
            throw new RuntimeException("Status is required");
        }
        return ResponseEntity.ok(ApiResponse.success(
                userStatusService.setStatus(user.getId(), request.status(), request.clientName())));
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userStatusService.getStatus(userId)));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
