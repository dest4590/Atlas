package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.clients.dto.response.ClientResponse;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.friends.service.FriendshipService;
import org.collapseloader.atlas.domain.users.dto.request.UpdateProfileRequest;
import org.collapseloader.atlas.domain.users.dto.response.PublicUserResponse;
import org.collapseloader.atlas.domain.users.dto.response.SocialLinkResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserMeResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserProfileResponse;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.SocialLink;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.entity.UserProfile;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.exception.ConflictException;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserStatusService userStatusService;
    private final UserAvatarStorageService userAvatarStorageService;
    private final FriendshipService friendshipService;
    private final ClientRepository clientRepository;

    public UserProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserStatusService userStatusService,
            UserAvatarStorageService userAvatarStorageService,
            FriendshipService friendshipService,
            ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userStatusService = userStatusService;
        this.userAvatarStorageService = userAvatarStorageService;
        this.friendshipService = friendshipService;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public UserMeResponse getMe(User principal) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var profile = ensureProfile(user);
        return mapUserMe(user, profile);
    }

    @Transactional(readOnly = true)
    public PublicUserResponse getPublicUser(User principal, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var profile = userProfileRepository.findByUserId(userId).orElse(null);
        String friendshipStatus = friendshipService.getFriendshipStatus(principal, userId);
        return new PublicUserResponse(
                user.getId(),
                user.getUsername(),
                mapProfile(profile),
                userStatusService.getStatus(userId),
                friendshipStatus,
                null,
                null);
    }

    @Transactional
    public UserProfileResponse updateProfile(User principal, UpdateProfileRequest request) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var profile = ensureProfile(user);
        String nickname = normalizeNickname(request.nickname());

        if (nickname != null) {
            String normalized = nickname.toLowerCase(Locale.ROOT);
            boolean exists = profile.getId() == null
                    ? userProfileRepository.existsByNickname(normalized)
                    : userProfileRepository.existsByNicknameAndIdNot(normalized, profile.getId());
            if (exists) {
                throw new ConflictException("Nickname is already in use");
            }
        }

        profile.setNickname(nickname);

        if (request.favoriteClientId() != null) {
            Client client = clientRepository.findById(request.favoriteClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found"));
            profile.setFavoriteClient(client);
        } else if (request.nickname() == null) {
            profile.setFavoriteClient(null);
        }

        var savedProfile = userProfileRepository.save(profile);
        return mapProfile(savedProfile);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(User principal, MultipartFile avatar) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var profile = ensureProfile(user);
        String previousPath = profile.getAvatarPath();
        String storedPath = userAvatarStorageService.storeAvatar(user.getId(), avatar);
        profile.setAvatarPath(storedPath);
        var savedProfile = userProfileRepository.save(profile);
        if (previousPath != null && !previousPath.equals(storedPath)) {
            userAvatarStorageService.deleteAvatar(previousPath);
        }
        return mapProfile(savedProfile);
    }

    @Transactional
    public UserProfileResponse resetAvatar(User principal) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var profile = ensureProfile(user);
        String previousPath = profile.getAvatarPath();
        profile.setAvatarPath(null);
        var savedProfile = userProfileRepository.save(profile);
        if (previousPath != null) {
            userAvatarStorageService.deleteAvatar(previousPath);
        }
        return mapProfile(savedProfile);
    }

    private UserProfile ensureProfile(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    var profile = UserProfile.builder()
                            .user(user)
                            .role(ProfileRole.USER)
                            .build();
                    user.setProfile(profile);
                    return userProfileRepository.save(profile);
                });
    }

    private UserMeResponse mapUserMe(User user, UserProfile profile) {
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                mapProfile(profile),
                userStatusService.getStatus(user.getId()));
    }

    private UserProfileResponse mapProfile(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return new UserProfileResponse(
                profile.getId(),
                profile.getNickname(),
                profile.getAvatarUrl(),
                profile.getRole(),
                mapSocialLinks(profile.getSocialLinks()),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                mapClient(profile.getFavoriteClient()));
    }

    private List<SocialLinkResponse> mapSocialLinks(List<SocialLink> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        return links.stream()
                .map(link -> new SocialLinkResponse(link.getPlatform(), link.getUrl()))
                .toList();
    }

    private ClientResponse mapClient(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getVersion() != null ? client.getVersion().name() : null,
                client.getFilename(),
                client.getMd5Hash(),
                client.getSize(),
                client.getMainClass(),
                client.isShow(),
                client.isWorking(),
                client.getLaunches(),
                client.getDownloads(),
                client.getType() != null ? client.getType().name() : null,
                client.getCreatedAt());
    }

    private String normalizeNickname(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
