package org.collapseloader.atlas.domain.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.audit.AuditLog;
import org.collapseloader.atlas.domain.audit.AuditLogService;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.news.News;
import org.collapseloader.atlas.domain.news.NewsRepository;
import org.collapseloader.atlas.domain.news.NewsRequest;
import org.collapseloader.atlas.domain.news.NewsService;
import org.collapseloader.atlas.domain.users.dto.request.AdminUserUpdateRequest;
import org.collapseloader.atlas.domain.users.dto.response.AdminUserDetailResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserAdminResponse;
import org.collapseloader.atlas.domain.users.entity.*;
import org.collapseloader.atlas.domain.users.repository.UserPreferenceRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.service.UserStatusService;
import org.collapseloader.atlas.domain.users.service.UsernameValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final NewsService newsService;
    private final AuditLogService auditLogService;
    private final ClientRepository clientRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final AchievementService achievementService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserStatusService userStatusService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("users", userRepository.count());
        stats.put("news", newsRepository.count());
        stats.put("clients", clientRepository.count());
        stats.put("online", userStatusService.getOnlineUserCount());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserAdminResponse>> getUsers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String profileRole,
            @RequestParam(required = false) Boolean enabled) {

        org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), likePattern),
                        cb.like(cb.lower(root.get("email")), likePattern)));
            }

            if (role != null && !role.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("role"), Role.valueOf(role)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (profileRole != null && !profileRole.isBlank()) {
                try {
                    predicates.add(cb.equal(root.join("profile").get("role"), ProfileRole.valueOf(profileRole)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        org.springframework.data.domain.Page<User> userPage = userRepository.findAll(spec, pageable);

        return ResponseEntity.ok(userPage.map(u -> new UserAdminResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole().name(),
                u.getProfile() != null ? u.getProfile().getRole().name() : "USER",
                u.isEnabled(),
                u.getCreatedAt())));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminUserDetailResponse> getUserDetails(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found"));

        List<UserPreference> prefs = userPreferenceRepository.findByUserId(user.getId());

        UserProfile profile = user.getProfile();

        var profileDto = (profile != null) ? new AdminUserDetailResponse.UserProfileDto(
                profile.getNickname(),
                profile.getAvatarPath(),
                profile.getRole(),
                profile.getSocialLinks().stream()
                        .map(l -> new AdminUserDetailResponse.SocialLinkDto(l.getId(), l.getPlatform(), l.getUrl()))
                        .collect(Collectors.toList()))
                : null;

        var prefDtos = prefs.stream()
                .map(p -> new AdminUserDetailResponse.UserPreferenceDto(p.getId(), p.getKey(),
                        p.getValue() != null ? p.getValue().toString() : "null"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                profileDto,
                prefDtos));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found"));

        if (request.username() != null)
            user.setUsername(request.username());
        if (request.role() != null)
            user.setRole(Role.valueOf(request.role()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setEnabled(request.enabled());

        if (user.getProfile() == null) {
            user.setProfile(UserProfile.builder().user(user).build());
        }
        UserProfile profile = user.getProfile();
        if (request.nickname() != null) {
            UsernameValidator.validate(request.nickname());
            profile.setNickname(request.nickname());
        }
        if (request.avatarPath() != null)
            profile.setAvatarPath(request.avatarPath());
        if (request.profileRole() != null)
            profile.setRole(request.profileRole());

        if (profile.getRole() != null && profile.getRole().isTester()) {
            achievementService.unlockAchievement(user.getId(), "BETA_TESTER");
        }

        if (request.socialLinks() != null) {
            Map<SocialPlatform, SocialLink> existingMap = new HashMap<>();
            for (SocialLink link : profile.getSocialLinks()) {
                if (link.getPlatform() != null) {
                    existingMap.put(link.getPlatform(), link);
                }
            }

            Set<SocialLink> linksToKeep = new HashSet<>();
            for (var linkReq : request.socialLinks()) {
                if (linkReq.platform() == null)
                    continue;

                SocialLink existing = existingMap.get(linkReq.platform());
                if (existing != null) {
                    existing.setUrl(linkReq.url());
                    linksToKeep.add(existing);
                } else {
                    SocialLink newLink = SocialLink.builder()
                            .platform(linkReq.platform())
                            .url(linkReq.url())
                            .profile(profile)
                            .build();
                    profile.getSocialLinks().add(newLink);
                    linksToKeep.add(newLink);
                }
            }

            profile.getSocialLinks().removeIf(link -> !linksToKeep.contains(link));
        }

        if (request.preferences() != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (var prefReq : request.preferences()) {
                JsonNode valueNode = mapper.valueToTree(prefReq.value());
                var existing = userPreferenceRepository.findByUserIdAndKey(user.getId(), prefReq.key());
                if (existing.isPresent()) {
                    existing.get().setValue(valueNode);
                    userPreferenceRepository.save(existing.get());
                } else {
                    userPreferenceRepository.save(UserPreference.builder()
                            .user(user)
                            .key(prefReq.key())
                            .value(valueNode)
                            .build());
                }
            }
        }

        userRepository.save(user);
        String logMessage = "Updated user details for " + user.getUsername();
        if (request.password() != null && !request.password().isBlank()) {
            logMessage += " (including password reset)";
        }
        auditLogService.log("UPDATE_USER", "USER", user.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                logMessage);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log("RESET_PASSWORD", "USER", user.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName(),
                "Reset password for user " + user.getUsername());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<News>> getNews() {
        return ResponseEntity.ok(newsRepository.findAll());
    }

    @PostMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<News> createNews(
            @RequestBody NewsRequest request) {
        var news = newsService.createNews(request);
        auditLogService.log("CREATE_NEWS", "NEWS", news.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Created news article: " + news.getTitle());
        return ResponseEntity.ok(news);
    }

    @PutMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<News> updateNews(@PathVariable Long id,
                                           @RequestBody NewsRequest request) {
        var news = newsService.updateNews(id, request);
        auditLogService.log("UPDATE_NEWS", "NEWS", news.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Updated news article: " + news.getTitle());
        return ResponseEntity.ok(news);
    }

    @DeleteMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        var news = newsRepository.findById(id).orElse(null);
        String title = news != null ? news.getTitle() : "Unknown";
        newsService.deleteNews(id);
        auditLogService.log("DELETE_NEWS", "NEWS", id.toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Deleted news article: " + title);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogs(pageable));
    }

    @PostMapping("/users/{userId}/achievements/{achievementKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> grantAchievement(@PathVariable Long userId, @PathVariable String achievementKey) {
        achievementService.unlockAchievement(userId, achievementKey);
        auditLogService.log("GRANT_ACHIEVEMENT", "USER", userId.toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName(),
                "Granted achievement " + achievementKey);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/achievements/{achievementKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeAchievement(@PathVariable Long userId, @PathVariable String achievementKey) {
        achievementService.revokeAchievement(userId, achievementKey);
        auditLogService.log("REVOKE_ACHIEVEMENT", "USER", userId.toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName(),
                "Revoked achievement " + achievementKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clients/trigger-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> triggerUpdate() {
        Map<String, String> payload = new HashMap<>();
        payload.put("command", "CHECK_FOR_UPDATES");
        messagingTemplate.convertAndSend("/topic/commands", payload);
        auditLogService.log("TRIGGER_UPDATE", "SYSTEM", "ALL",
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName(),
                "Triggered manual update check for all clients");
        return ResponseEntity.ok().build();
    }
}
