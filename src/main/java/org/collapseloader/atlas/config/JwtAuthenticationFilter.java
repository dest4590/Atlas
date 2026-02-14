package org.collapseloader.atlas.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.service.JwtService;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final org.collapseloader.atlas.domain.users.service.TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService,
                                   org.collapseloader.atlas.domain.users.service.TokenBlacklistService tokenBlacklistService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && path.startsWith("/api/v1/auth/");
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();
        if (jwt.isEmpty() || jwt.chars().filter(ch -> ch == '.').count() != 2) {
            filterChain.doFilter(request, response);
            return;
        }

        String username;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (JwtException | IllegalArgumentException ignored) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                try {
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        var user = userRepository.findByUsername(username).orElse(null);
                        if (user != null) {
                            var authToken = new UsernamePasswordAuthenticationToken(user, null,
                                    userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                } catch (JwtException | IllegalArgumentException ignored) {
                }
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
