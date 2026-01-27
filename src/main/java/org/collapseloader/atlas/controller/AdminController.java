package org.collapseloader.atlas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.news.NewsRepository;
import org.collapseloader.atlas.domain.users.dto.request.AdminUserUpdateRequest;
import org.collapseloader.atlas.domain.users.dto.response.AdminUserDetailResponse;
import org.collapseloader.atlas.domain.users.dto.response.UserAdminResponse;
import org.collapseloader.atlas.domain.users.entity.*;
import org.collapseloader.atlas.domain.users.repository.UserPreferenceRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final org.collapseloader.atlas.domain.news.NewsService newsService;
    private final org.collapseloader.atlas.domain.audit.AuditLogService auditLogService;
    private final ClientRepository clientRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final org.collapseloader.atlas.domain.achievements.service.AchievementService achievementService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("users", userRepository.count());
        stats.put("news", newsRepository.count());
        stats.put("clients", clientRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAdminResponse>> getUsers() {
        List<UserAdminResponse> users = userRepository.findAll().stream()
                .map(u -> new UserAdminResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.isEnabled(),
                        u.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminUserDetailResponse> getUserDetails(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
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
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found"));

        if (request.username() != null)
            user.setUsername(request.username());
        if (request.role() != null)
            user.setRole(Role.valueOf(request.role()));
        user.setEnabled(request.enabled());

        if (user.getProfile() == null) {
            user.setProfile(UserProfile.builder().user(user).build());
        }
        UserProfile profile = user.getProfile();
        if (request.nickname() != null)
            profile.setNickname(request.nickname());
        if (request.avatarPath() != null)
            profile.setAvatarPath(request.avatarPath());
        if (request.profileRole() != null)
            profile.setRole(request.profileRole());

        if (profile.getRole() != null && profile.getRole().isTester()) {
            achievementService.unlockAchievement(user.getId(), "BETA_TESTER");
        }

        if (request.socialLinks() != null) {
            profile.getSocialLinks().clear();
            for (var linkReq : request.socialLinks()) {
                SocialLink link = SocialLink.builder()
                        .platform(linkReq.platform())
                        .url(linkReq.url())
                        .profile(profile)
                        .build();
                profile.getSocialLinks().add(link);
            }
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
        auditLogService.log("UPDATE_USER", "USER", user.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Updated user details for " + user.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<org.collapseloader.atlas.domain.news.News>> getNews() {
        return ResponseEntity.ok(newsRepository.findAll());
    }

    @PostMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.collapseloader.atlas.domain.news.News> createNews(
            @RequestBody org.collapseloader.atlas.domain.news.dto.request.NewsRequest request) {
        var news = newsService.createNews(request);
        auditLogService.log("CREATE_NEWS", "NEWS", news.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Created news article: " + news.getTitle());
        return ResponseEntity.ok(news);
    }

    @PutMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.collapseloader.atlas.domain.news.News> updateNews(@PathVariable Long id,
                                                                                @RequestBody org.collapseloader.atlas.domain.news.dto.request.NewsRequest request) {
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
    public ResponseEntity<org.springframework.data.domain.Page<org.collapseloader.atlas.domain.audit.AuditLog>> getAuditLogs(
            @org.springframework.data.web.PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogs(pageable));
    }
}
