package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.users.dto.request.SocialLinkRequest;
import org.collapseloader.atlas.domain.users.dto.request.UpdateSocialLinksRequest;
import org.collapseloader.atlas.domain.users.dto.response.SocialLinkResponse;
import org.collapseloader.atlas.domain.users.entity.*;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class UserSocialLinksService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserSocialLinksService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<SocialLinkResponse> getSocialLinks(User principal) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        var profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return mapSocialLinks(profile == null ? null : profile.getSocialLinks());
    }

    @Transactional
    public List<SocialLinkResponse> replaceSocialLinks(User principal, UpdateSocialLinksRequest request) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        var profile = ensureProfile(user);
        if (profile.getSocialLinks() == null) {
            profile.setSocialLinks(new ArrayList<>());
        } else {
            profile.getSocialLinks().clear();
        }

        if (request != null && request.links() != null) {
            var uniqueLinks = new LinkedHashMap<SocialPlatform, SocialLinkRequest>();
            for (var link : request.links()) {
                if (link == null || link.platform() == null) {
                    continue;
                }
                String url = normalizeUrl(link.url());
                if (url == null) {
                    continue;
                }
                uniqueLinks.put(link.platform(), new SocialLinkRequest(link.platform(), url));
            }
            for (var link : uniqueLinks.values()) {
                var entity = SocialLink.builder()
                        .platform(link.platform())
                        .url(link.url())
                        .profile(profile)
                        .build();
                profile.getSocialLinks().add(entity);
            }
        }

        var savedProfile = userProfileRepository.save(profile);
        return mapSocialLinks(savedProfile.getSocialLinks());
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

    private List<SocialLinkResponse> mapSocialLinks(List<SocialLink> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        return links.stream()
                .map(link -> new SocialLinkResponse(link.getPlatform(), link.getUrl()))
                .toList();
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
