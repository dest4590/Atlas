package org.collapseloader.atlas.domain.friends.service;

import org.collapseloader.atlas.domain.friends.dto.response.FriendRequestResponse;
import org.collapseloader.atlas.domain.friends.dto.response.FriendResponse;
import org.collapseloader.atlas.domain.friends.entity.FriendRequest;
import org.collapseloader.atlas.domain.friends.entity.FriendRequestStatus;
import org.collapseloader.atlas.domain.friends.repository.FriendRequestRepository;
import org.collapseloader.atlas.domain.users.dto.response.SearchUserResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.service.UserStatusService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FriendshipService {
    private static final int MAX_SEARCH_LIMIT = 50;

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final UserStatusService userStatusService;

    public FriendshipService(
            FriendRequestRepository friendRequestRepository,
            UserRepository userRepository,
            UserStatusService userStatusService
    ) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.userStatusService = userStatusService;
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(User principal) {
        var requests = friendRequestRepository.findByStatusForUser(principal.getId(), FriendRequestStatus.ACCEPTED);
        return requests.stream()
                .map(request -> mapFriendResponse(principal, request))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getRequests(User principal, RequestType type) {
        List<FriendRequest> requests = switch (type) {
            case INCOMING ->
                    friendRequestRepository.findIncomingRequests(principal.getId(), FriendRequestStatus.PENDING);
            case OUTGOING ->
                    friendRequestRepository.findOutgoingRequests(principal.getId(), FriendRequestStatus.PENDING);
            case ALL -> friendRequestRepository.findByStatusForUser(principal.getId(), FriendRequestStatus.PENDING);
        };
        return requests.stream()
                .map(this::mapRequestResponse)
                .toList();
    }

    @Transactional
    public FriendRequestResponse sendRequest(User principal, Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
        if (principal.getId().equals(userId)) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }
        var target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var existing = friendRequestRepository.findBetweenUsers(principal.getId(), userId);
        if (existing.isPresent()) {
            var request = existing.get();
            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new RuntimeException("Already friends");
            }
            if (request.getStatus() == FriendRequestStatus.PENDING) {
                if (request.getRequester().getId().equals(principal.getId())) {
                    throw new RuntimeException("Friend request already sent");
                }
                throw new RuntimeException("Friend request already received");
            }
            throw new RuntimeException("User is blocked");
        }

        var request = FriendRequest.builder()
                .requester(principal)
                .addressee(target)
                .status(FriendRequestStatus.PENDING)
                .build();
        var saved = friendRequestRepository.save(request);
        return mapRequestResponse(saved);
    }

    @Transactional
    public FriendRequestResponse acceptRequest(User principal, Long requestId) {
        var request = friendRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        if (!request.getAddressee().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setBlockedBy(null);
        return mapRequestResponse(friendRequestRepository.save(request));
    }

    @Transactional
    public void declineRequest(User principal, Long requestId) {
        var request = friendRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        var isRequester = request.getRequester().getId().equals(principal.getId());
        var isAddressee = request.getAddressee().getId().equals(principal.getId());
        if (!isRequester && !isAddressee) {
            throw new RuntimeException("Unauthorized");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }
        friendRequestRepository.delete(request);
    }

    @Transactional
    public FriendRequestResponse blockUser(User principal, Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
        if (principal.getId().equals(userId)) {
            throw new RuntimeException("Cannot block yourself");
        }
        var target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var request = friendRequestRepository.findBetweenUsers(principal.getId(), userId)
                .orElseGet(() -> FriendRequest.builder()
                        .requester(principal)
                        .addressee(target)
                        .build());

        request.setStatus(FriendRequestStatus.BLOCKED);
        request.setBlockedBy(principal);
        var saved = friendRequestRepository.save(request);
        return mapRequestResponse(saved);
    }

    @Transactional
    public FriendRequestResponse unblockUser(User principal, Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
        var request = friendRequestRepository.findBetweenUsers(principal.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        if (request.getStatus() != FriendRequestStatus.BLOCKED) {
            throw new RuntimeException("Not blocked");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setBlockedBy(null);

        var saved = friendRequestRepository.save(request);
        return mapRequestResponse(saved);
    }

    @Transactional
    public void removeFriend(User principal, Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
        var request = friendRequestRepository.findBetweenUsers(principal.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        if (request.getStatus() != FriendRequestStatus.ACCEPTED) {
            throw new RuntimeException("Not friends");
        }
        friendRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<SearchUserResponse> searchUsers(User principal, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), MAX_SEARCH_LIMIT);
        var users = userRepository.searchUsers(query.trim(), PageRequest.of(0, size));
        if (users.isEmpty()) {
            return List.of();
        }
        var filtered = users.stream()
                .filter(user -> !user.getId().equals(principal.getId()))
                .toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        var userIds = filtered.stream().map(User::getId).toList();
        var requests = friendRequestRepository.findAllBetweenUserAndOthers(principal.getId(), userIds);
        var statusByUserId = buildFriendshipStatusMap(principal, requests);

        return filtered.stream()
                .map(user -> new SearchUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getProfile() == null ? null : user.getProfile().getNickname(),
                        statusByUserId.get(user.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getFriendshipStatus(User principal, Long otherUserId) {
        if (principal == null || otherUserId == null || principal.getId().equals(otherUserId)) {
            return null;
        }
        return friendRequestRepository.findBetweenUsers(principal.getId(), otherUserId)
                .map(request -> toFriendshipStatus(principal, request))
                .orElse(null);
    }

    private FriendResponse mapFriendResponse(User principal, FriendRequest request) {
        User friend = request.getRequester().getId().equals(principal.getId())
                ? request.getAddressee()
                : request.getRequester();
        return new FriendResponse(
                friend.getId(),
                friend.getUsername(),
                friend.getProfile() == null ? null : friend.getProfile().getNickname(),
                userStatusService.getStatus(friend.getId())
        );
    }

    private FriendRequestResponse mapRequestResponse(FriendRequest request) {
        return new FriendRequestResponse(
                request.getId(),
                new FriendResponse(
                        request.getRequester().getId(),
                        request.getRequester().getUsername(),
                        request.getRequester().getProfile() == null ? null : request.getRequester().getProfile().getNickname(),
                        userStatusService.getStatus(request.getRequester().getId())
                ),
                new FriendResponse(
                        request.getAddressee().getId(),
                        request.getAddressee().getUsername(),
                        request.getAddressee().getProfile() == null ? null : request.getAddressee().getProfile().getNickname(),
                        userStatusService.getStatus(request.getAddressee().getId())
                ),
                request.getStatus().name().toLowerCase(Locale.ROOT),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private Map<Long, String> buildFriendshipStatusMap(User principal, List<FriendRequest> requests) {
        Map<Long, String> statuses = new HashMap<>();
        for (var request : requests) {
            Long otherId = request.getRequester().getId().equals(principal.getId())
                    ? request.getAddressee().getId()
                    : request.getRequester().getId();
            statuses.put(otherId, toFriendshipStatus(principal, request));
        }
        return statuses;
    }

    private String toFriendshipStatus(User principal, FriendRequest request) {
        return switch (request.getStatus()) {
            case ACCEPTED -> "friends";
            case PENDING -> request.getRequester().getId().equals(principal.getId())
                    ? "request_sent"
                    : "request_received";
            case BLOCKED -> "blocked";
        };
    }

    public enum RequestType {
        INCOMING,
        OUTGOING,
        ALL;

        public static RequestType from(String value) {
            if (value == null || value.isBlank()) {
                return INCOMING;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "outgoing", "sent" -> OUTGOING;
                case "all" -> ALL;
                default -> INCOMING;
            };
        }
    }
}
