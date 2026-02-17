package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.achievements.service.AchievementService;
import org.collapseloader.atlas.domain.users.dto.request.AuthRequest;
import org.collapseloader.atlas.domain.users.dto.request.AuthSetPasswordRequest;
import org.collapseloader.atlas.domain.users.dto.response.AuthResponse;
import org.collapseloader.atlas.domain.users.entity.*;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.repository.VerificationTokenRepository;
import org.collapseloader.atlas.exception.ConflictException;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.collapseloader.atlas.exception.ValidationException;
import org.collapseloader.atlas.service.EmailService;
import org.collapseloader.atlas.util.passwords.HybridPasswordEncoder;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserStatusService userStatusService;
    private final org.collapseloader.atlas.domain.achievements.service.AchievementService achievementService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserStatusService userStatusService,
            AchievementService achievementService,
            TokenBlacklistService tokenBlacklistService,
            EmailService emailService,
            VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userStatusService = userStatusService;
        this.achievementService = achievementService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    public AuthResponse register(AuthRequest request) {
        UsernameValidator.validate(request.username());

        if (userRepository.existsByUsernameIgnoreCase(request.username().trim())) {
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

        String token = java.util.UUID.randomUUID().toString();
        var verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(Instant.now().plus(java.time.Duration.ofHours(24)))
                .build();
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return new AuthResponse(null);
    }

    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new ValidationException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        var user = userRepository.findByUsernameIgnoreCase(request.username())
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

        var access = jwtService.generateAccessToken(user);
        return new AuthResponse(access);
    }

    public AuthResponse setPassword(User user, AuthSetPasswordRequest password) {
        if (!passwordEncoder.matches(password.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(password.newPassword()));
        userRepository.save(user);

        var access = jwtService.generateAccessToken(user);
        return new AuthResponse(access);
    }

    public void logout(String token) {
        String username = jwtService.extractUsername(token);
        if (username != null) {
            userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                userStatusService.setStatus(user.getId(), UserStatus.OFFLINE, null);

                try {
                    Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
                    if (expiration != null) {
                        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                        if (remainingMillis > 0) {
                            tokenBlacklistService.blacklistToken(token, remainingMillis);
                        }
                    }
                } catch (Exception ignored) {

                }
            });
        }
    }
}
