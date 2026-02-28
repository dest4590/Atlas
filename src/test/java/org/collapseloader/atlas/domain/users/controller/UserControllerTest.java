package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.friends.service.FriendshipService;
import org.collapseloader.atlas.domain.presets.service.PresetService;
import org.collapseloader.atlas.domain.users.dto.response.PublicUserResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserMeResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserExternalAccountsService;
import org.collapseloader.atlas.domain.users.service.UserFavoritesService;
import org.collapseloader.atlas.domain.users.service.UserPreferencesService;
import org.collapseloader.atlas.domain.users.service.UserProfileService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserProfileService userProfileService;
    private FriendshipService friendshipService;
    private UserPreferencesService userPreferencesService;
    private UserFavoritesService userFavoritesService;
    private UserExternalAccountsService userExternalAccountsService;
    private AchievementService achievementService;
    private PresetService presetService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userProfileService = mock(UserProfileService.class);
        friendshipService = mock(FriendshipService.class);
        userPreferencesService = mock(UserPreferencesService.class);
        userFavoritesService = mock(UserFavoritesService.class);
        userExternalAccountsService = mock(UserExternalAccountsService.class);
        achievementService = mock(AchievementService.class);
        presetService = mock(PresetService.class);

        userController = new UserController(
                userProfileService,
                friendshipService,
                userPreferencesService,
                userFavoritesService,
                userExternalAccountsService,
                achievementService,
                presetService
        );
    }

    @Test
    void getMeWithoutAuthenticationThrowsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> userController.getMe(null));
    }

    @Test
    void getMeWithAuthenticationReturnsSuccess() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        var me = new UserMeResponse(
                1L,
                "tester",
                "tester@example.com",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null,
                null
        );
        when(userProfileService.getMe(user)).thenReturn(me);

        ResponseEntity<?> response = userController.getMe(auth);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        var envelope = (ApiResponse<?>) response.getBody();
        assertEquals(true, envelope.success());
        verify(userProfileService).getMe(user);
    }

    @Test
    void getInitAggregatesAllServices() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);

        when(userProfileService.getMe(user)).thenReturn(null);
        when(userPreferencesService.getPreferences(user)).thenReturn(null);
        when(userFavoritesService.getFavorites(user)).thenReturn(null);
        when(userExternalAccountsService.getExternalAccounts(user)).thenReturn(null);
        when(friendshipService.getFriends(user)).thenReturn(Collections.emptyList());
        when(friendshipService.getRequests(user, FriendshipService.RequestType.OUTGOING)).thenReturn(Collections.emptyList());
        when(friendshipService.getRequests(user, FriendshipService.RequestType.INCOMING)).thenReturn(Collections.emptyList());
        when(friendshipService.getRequests(user, FriendshipService.RequestType.BLOCKED)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getInit(auth);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        var envelope = (ApiResponse<?>) response.getBody();
        assertEquals(true, envelope.success());
        verify(userProfileService).getMe(user);
        verify(userPreferencesService).getPreferences(user);
        verify(userFavoritesService).getFavorites(user);
        verify(userExternalAccountsService).getExternalAccounts(user);
        verify(friendshipService).getFriends(user);
        verify(friendshipService).getRequests(user, FriendshipService.RequestType.OUTGOING);
        verify(friendshipService).getRequests(user, FriendshipService.RequestType.INCOMING);
        verify(friendshipService).getRequests(user, FriendshipService.RequestType.BLOCKED);
    }

    @Test
    void getUserWithAchievementsIncludeCallsAchievementServiceOnly() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        var basic = new PublicUserResponse(2L, "other", null, null, "NONE", null, null);

        when(userProfileService.getPublicUser(user, 2L)).thenReturn(basic);
        when(achievementService.getUserAchievements(2L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getUser(auth, 2L, Set.of("achievements"));

        assertEquals(200, response.getStatusCode().value());
        verify(achievementService).getUserAchievements(2L);
        verify(presetService, never()).listPresets(any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void getUserWithPresetsIncludeCallsPresetServiceOnly() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        var basic = new PublicUserResponse(2L, "other", null, null, "NONE", null, null);

        when(userProfileService.getPublicUser(user, 2L)).thenReturn(basic);
        when(presetService.listPresets(user, null, 2L, "newest", 100)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getUser(auth, 2L, Set.of("presets"));

        assertEquals(200, response.getStatusCode().value());
        verify(presetService).listPresets(user, null, 2L, "newest", 100);
        verify(achievementService, never()).getUserAchievements(anyLong());
    }

    @Test
    void getUserWithoutIncludeSkipsOptionalLoads() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        var basic = new PublicUserResponse(2L, "other", null, null, "NONE", null, null);

        when(userProfileService.getPublicUser(user, 2L)).thenReturn(basic);

        ResponseEntity<?> response = userController.getUser(auth, 2L, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(userProfileService).getPublicUser(user, 2L);
        verify(achievementService, never()).getUserAchievements(anyLong());
        verify(presetService, never()).listPresets(any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void searchUsersUsesProvidedLimit() {
        var user = user();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        when(friendshipService.searchUsers(user, "abc", 7)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.searchUsers(auth, "abc", 7);

        assertEquals(200, response.getStatusCode().value());
        verify(friendshipService).searchUsers(user, "abc", 7);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("tester")
                .password("hashed")
                .email("tester@example.com")
                .build();
    }
}
