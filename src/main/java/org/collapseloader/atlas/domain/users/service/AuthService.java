package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.users.dto.request.AuthRequest;
import org.collapseloader.atlas.domain.users.dto.request.AuthSetPasswordRequest;
import org.collapseloader.atlas.domain.users.dto.response.AuthResponse;
import org.collapseloader.atlas.domain.users.entity.*;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.exception.ConflictException;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.collapseloader.atlas.exception.ValidationException;
import org.collapseloader.atlas.util.passwords.HybridPasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserStatusService userStatusService;
    private final AchievementService achievementService;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserStatusService userStatusService,
            AchievementService achievementService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userStatusService = userStatusService;
        this.achievementService = achievementService;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ConflictException("User already exists");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationException("Email is required");
        }

        var user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole(Role.USER);

        var savedUser = userRepository.save(user);
        var profile = UserProfile.builder()
                .user(savedUser)
                .role(ProfileRole.USER)
                .build();
        savedUser.setProfile(profile);
        userProfileRepository.save(profile);

        var jwt = jwtService.generateToken(savedUser);
        return new AuthResponse(jwt);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        var user = userRepository.findByUsername(request.username())
                .orElseThrow();

        if (passwordEncoder instanceof HybridPasswordEncoder hybridEncoder
                && hybridEncoder.isDjangoHash(user.getPassword())) {
            user.setPassword(hybridEncoder.encode(request.password()));
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        userStatusService.setStatus(user.getId(), UserStatus.ONLINE, null);

        var profile = user.getProfile();

        if (profile != null && profile.getRole() != null && profile.getRole().isTester()) {
            achievementService.unlockAchievement(user.getId(), "BETA_TESTER");
        }

        var jwt = jwtService.generateToken(user);
        return new AuthResponse(jwt);
    }

    public AuthResponse setPassword(User user, AuthSetPasswordRequest password) {
        if (!passwordEncoder.matches(password.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(password.newPassword()));
        userRepository.save(user);

        var jwt = jwtService.generateToken(user);
        return new AuthResponse(jwt);
    }
}
