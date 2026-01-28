package org.collapseloader.atlas.domain.friends.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.friends.dto.request.FriendRequestCreateRequest;
import org.collapseloader.atlas.domain.friends.dto.response.FriendRequestResponse;
import org.collapseloader.atlas.domain.friends.dto.response.FriendRequestsBatchResponse;
import org.collapseloader.atlas.domain.friends.dto.response.FriendResponse;
import org.collapseloader.atlas.domain.friends.dto.response.FriendsBatchResponse;
import org.collapseloader.atlas.domain.friends.service.FriendshipService;
import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/friends"})
@PreAuthorize("isAuthenticated()")
public class FriendsController {
    private final FriendshipService friendshipService;

    public FriendsController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getFriends(user)));
    }

    @GetMapping({"/batch"})
    public ResponseEntity<ApiResponse<FriendsBatchResponse>> getBatch(Authentication authentication) {
        var user = requireUser(authentication);
        var friends = friendshipService.getFriends(user);
        var sent = friendshipService.getRequests(user, FriendshipService.RequestType.OUTGOING);
        var received = friendshipService.getRequests(user, FriendshipService.RequestType.INCOMING);
        var blocked = friendshipService.getRequests(user, FriendshipService.RequestType.BLOCKED);
        var requests = new FriendRequestsBatchResponse(sent, received, blocked);
        return ResponseEntity.ok(ApiResponse.success(new FriendsBatchResponse(friends, requests)));
    }

    @GetMapping({"/requests"})
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getRequests(
            Authentication authentication,
            @RequestParam(value = "type", required = false) String type) {
        var user = requireUser(authentication);
        var requestType = FriendshipService.RequestType.from(type);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getRequests(user, requestType)));
    }

    @PostMapping({"/requests"})
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(
            Authentication authentication,
            @RequestBody FriendRequestCreateRequest request) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.sendRequest(user, request.userId())));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> acceptRequest(
            Authentication authentication,
            @PathVariable Long requestId) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.acceptRequest(user, requestId)));
    }

    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            Authentication authentication,
            @PathVariable Long requestId) {
        var user = requireUser(authentication);
        friendshipService.declineRequest(user, requestId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/block")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> blockUser(
            Authentication authentication,
            @RequestBody FriendRequestCreateRequest request) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.blockUser(user, request.userId())));
    }

    @PostMapping("/unblock")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> unblockUser(
            Authentication authentication,
            @RequestBody FriendRequestCreateRequest request) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(friendshipService.unblockUser(user, request.userId())));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            Authentication authentication,
            @PathVariable Long userId) {
        var user = requireUser(authentication);
        friendshipService.removeFriend(user, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
