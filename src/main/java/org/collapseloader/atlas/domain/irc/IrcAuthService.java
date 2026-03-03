package org.collapseloader.atlas.domain.irc;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.users.entity.ProfileRole;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.service.JwtService;
import org.collapseloader.atlas.domain.users.service.TokenBlacklistService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IrcAuthService {
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public AuthResult authenticate(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("null")) {
            return AuthResult.guest();
        }

        if (tokenBlacklistService.isBlacklisted(normalized)) {
            return AuthResult.guest();
        }

        String username = jwtService.extractUsername(normalized);
        if (username == null || username.isBlank()) {
            return AuthResult.guest();
        }

        var users = userRepository.findAllByUsernameIgnoreCase(username);
        if (users.size() != 1) {
            return AuthResult.guest();
        }

        User user = users.getFirst();
        if (!jwtService.isTokenValid(normalized, user)) {
            return AuthResult.guest();
        }

        String role = "user";
        var profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null && profile.getRole() != null) {
            role = normalizeProfileRole(profile.getRole());
        } else if (user.getRole() != null) {
            role = user.getRole().name().toLowerCase(Locale.ROOT);
        }

        return new AuthResult(true, user.getId(), user.getUsername(), role);
    }

    private String normalizeProfileRole(ProfileRole role) {
        if (role == null) {
            return "user";
        }
        return role.name().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(boolean authenticated, Long userId, String username, String role) {
        static AuthResult guest() {
            return new AuthResult(false, null, null, "guest");
        }
    }
}
