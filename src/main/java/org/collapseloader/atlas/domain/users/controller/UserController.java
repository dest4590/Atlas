package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.friends.service.FriendshipService;
import org.collapseloader.atlas.domain.users.dto.request.UpdateProfileRequest;
import org.collapseloader.atlas.domain.users.dto.response.PublicUserResponse;
import org.collapseloader.atlas.domain.users.dto.response.SearchUserResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserMeResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserProfileResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileService userProfileService;
    private final FriendshipService friendshipService;

    public UserController(UserProfileService userProfileService, FriendshipService friendshipService) {
        this.userProfileService = userProfileService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMe(user)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getUser(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getPublicUser(user, userId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchUserResponse>>> searchUsers(
            Authentication authentication,
            @RequestParam("q") String query,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.searchUsers(user, query, limit)));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateProfile(user, request)));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
            Authentication authentication,
            @RequestParam("avatar") MultipartFile avatar
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.uploadAvatar(user, avatar)));
    }

    @PostMapping("/me/avatar/reset")
    public ResponseEntity<ApiResponse<UserProfileResponse>> resetAvatar(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.resetAvatar(user)));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
