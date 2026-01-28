package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.achievements.dto.UserAchievementResponse;
import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.friends.dto.response.FriendRequestsBatchResponse;
import org.collapseloader.atlas.domain.friends.dto.response.FriendsBatchResponse;
import org.collapseloader.atlas.domain.friends.service.FriendshipService;
import org.collapseloader.atlas.domain.presets.dto.response.PresetResponse;
import org.collapseloader.atlas.domain.presets.service.PresetService;
import org.collapseloader.atlas.domain.users.dto.request.UpdateProfileRequest;
import org.collapseloader.atlas.domain.users.dto.response.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserExternalAccountsService;
import org.collapseloader.atlas.domain.users.service.UserFavoritesService;
import org.collapseloader.atlas.domain.users.service.UserPreferencesService;
import org.collapseloader.atlas.domain.users.service.UserProfileService;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileService userProfileService;
    private final FriendshipService friendshipService;
    private final UserPreferencesService userPreferencesService;
    private final UserFavoritesService userFavoritesService;
    private final UserExternalAccountsService userExternalAccountsService;
    private final AchievementService achievementService;
    private final PresetService presetService;

    public UserController(
            UserProfileService userProfileService,
            FriendshipService friendshipService,
            UserPreferencesService userPreferencesService,
            UserFavoritesService userFavoritesService,
            UserExternalAccountsService userExternalAccountsService,
            AchievementService achievementService,
            PresetService presetService) {
        this.userProfileService = userProfileService;
        this.friendshipService = friendshipService;
        this.userPreferencesService = userPreferencesService;
        this.userFavoritesService = userFavoritesService;
        this.userExternalAccountsService = userExternalAccountsService;
        this.achievementService = achievementService;
        this.presetService = presetService;
    }

    @GetMapping("/init")
    public ResponseEntity<ApiResponse<UserInitResponse>> getInit(Authentication authentication) {
        var user = requireUser(authentication);
        var me = userProfileService.getMe(user);
        var preferences = userPreferencesService.getPreferences(user);
        var favorites = userFavoritesService.getFavorites(user);
        var accounts = userExternalAccountsService.getExternalAccounts(user);

        var friends = friendshipService.getFriends(user);
        var sent = friendshipService.getRequests(user, FriendshipService.RequestType.OUTGOING);
        var received = friendshipService.getRequests(user, FriendshipService.RequestType.INCOMING);
        var blocked = friendshipService.getRequests(user, FriendshipService.RequestType.BLOCKED);
        var friendRequests = new FriendRequestsBatchResponse(sent, received, blocked);
        var friendsBatch = new FriendsBatchResponse(friends, friendRequests);

        return ResponseEntity
                .ok(ApiResponse.success(new UserInitResponse(me, preferences, favorites, accounts, friendsBatch)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMe(user)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getUser(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestParam(required = false) Set<String> include) {
        var user = requireUser(authentication);
        var basic = userProfileService.getPublicUser(user, userId);
        var includes = include == null ? Collections.<String>emptySet() : include;

        List<UserAchievementResponse> achievements = null;
        if (includes.contains("achievements")) {
            achievements = achievementService.getUserAchievements(userId);
        }

        List<PresetResponse> presets = null;
        if (includes.contains("presets")) {
            presets = presetService.listPresets(user, null, userId, false, "newest", 100);
        }

        var response = new PublicUserResponse(
                basic.id(),
                basic.username(),
                basic.profile(),
                basic.status(),
                basic.friendshipStatus(),
                achievements,
                presets);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchUserResponse>>> searchUsers(
            Authentication authentication,
            @RequestParam("q") String query,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.searchUsers(user, query, limit)));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateProfile(user, request)));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
            Authentication authentication,
            @RequestParam("avatar") MultipartFile avatar) {
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
            throw new UnauthorizedException("Unauthorized");
        }
        return user;
    }
}
