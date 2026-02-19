package org.collapseloader.atlas.domain.users.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.collapseloader.atlas.domain.users.dto.request.AuthRequest;
import org.collapseloader.atlas.domain.users.dto.request.AuthSetPasswordRequest;
import org.collapseloader.atlas.domain.users.dto.response.AuthResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.AuthService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyEmail(token)));
    }

    @GetMapping("/verify-redirect")
    public void verifyRedirect(@RequestParam String code, @RequestParam String email,
                               HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect("collapseloader://verify?code=" + code + "&email=" + email);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request)
            throws BadRequestException {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/setPassword")
    public ResponseEntity<ApiResponse<AuthResponse>> setPassword(
            Authentication authentication,
            @Valid @RequestBody AuthSetPasswordRequest password) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(authService.setPassword(user, password)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(jakarta.servlet.http.HttpServletRequest request)
            throws BadRequestException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return user;
    }
}
