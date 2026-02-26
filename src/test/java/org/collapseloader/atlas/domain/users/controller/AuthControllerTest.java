package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.domain.users.dto.request.AuthRequest;
import org.collapseloader.atlas.domain.users.dto.request.AuthSetPasswordRequest;
import org.collapseloader.atlas.domain.users.dto.response.AuthResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.AuthService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

        private AuthService authService;
        private AuthController authController;

        @BeforeEach
        void setUp() {
                authService = mock(AuthService.class);
                authController = new AuthController(authService);
        }

        @Test
        void registerReturnsWrappedSuccess() {
                var request = new AuthRequest("tester", "password123", "tester@example.com");
                when(authService.register(request)).thenReturn(new AuthResponse("jwt-token"));

                ResponseEntity<?> response = authController.register(request);

                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                var envelope = (ApiResponse<?>) response.getBody();
                assertTrue(envelope.success());
                assertNotNull(envelope.data());
                verify(authService).register(request);
        }

        @Test
        void verifyEmailReturnsWrappedSuccess() {
                when(authService.verifyEmail("verify-token")).thenReturn(new AuthResponse("jwt-token"));

                ResponseEntity<?> response = authController.verifyEmail("verify-token");

                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                var envelope = (ApiResponse<?>) response.getBody();
                assertTrue(envelope.success());
                verify(authService).verifyEmail("verify-token");
        }

        @Test
        void resendVerificationReturnsWrappedSuccess() {
                ResponseEntity<?> response = authController.resendVerification("tester@example.com");

                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                var envelope = (ApiResponse<?>) response.getBody();
                assertTrue(envelope.success());
                verify(authService).resendVerification("tester@example.com");
        }

        @Test
        void loginReturnsWrappedSuccess() throws Exception {
                var request = new AuthRequest("tester", "password123", "tester@example.com");
                when(authService.login(request)).thenReturn(new AuthResponse("jwt-token"));

                ResponseEntity<?> response = authController.login(request);

                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                var envelope = (ApiResponse<?>) response.getBody();
                assertTrue(envelope.success());
                assertNotNull(envelope.data());
                verify(authService).login(request);
        }

        @Test
        void verifyRedirectBuildsDeepLinkUrl() throws Exception {
                var response = new MockHttpServletResponse();

                authController.verifyRedirect("abc", "user@example.com", response);

                assertEquals("collapseloader://verify?code=abc&email=user@example.com", response.getRedirectedUrl());
        }

        @Test
        void logoutWithBearerCallsService() throws Exception {
                var request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer test-token");

                ResponseEntity<?> response = authController.logout(request);

                assertEquals(200, response.getStatusCode().value());
                verify(authService).logout("test-token");
        }

        @Test
        void logoutWithoutBearerDoesNotCallService() throws Exception {
                var request = new MockHttpServletRequest();

                ResponseEntity<?> response = authController.logout(request);

                assertEquals(200, response.getStatusCode().value());
                verify(authService, never()).logout(any());
        }

        @Test
        void setPasswordWithoutAuthenticationThrowsUnauthorized() {
                var request = new AuthSetPasswordRequest("newpassword123", "oldpassword");

                assertThrows(UnauthorizedException.class, () -> authController.setPassword(null, request));
        }

        @Test
        void setPasswordWithAuthenticationReturnsSuccess() {
                var user = User.builder()
                                .id(1L)
                                .username("tester")
                                .password("hashed")
                                .email("tester@example.com")
                                .build();
                var authentication = new UsernamePasswordAuthenticationToken(user, null);
                var request = new AuthSetPasswordRequest("newpassword123", "oldpassword");
                when(authService.setPassword(eq(user), eq(request))).thenReturn(new AuthResponse("new-token"));

                ResponseEntity<?> response = authController.setPassword(authentication, request);

                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                var envelope = (ApiResponse<?>) response.getBody();
                assertTrue(envelope.success());
                verify(authService).setPassword(user, request);
        }

        @Test
        void setPasswordWithWrongPrincipalTypeThrowsUnauthorized() {
                var authentication = new UsernamePasswordAuthenticationToken("not-user", null);
                var request = new AuthSetPasswordRequest("newpassword123", "oldpassword");

                var exception = assertThrows(UnauthorizedException.class,
                                () -> authController.setPassword(authentication, request));

                assertTrue(exception.getMessage().contains("Unauthorized"));
        }
}
