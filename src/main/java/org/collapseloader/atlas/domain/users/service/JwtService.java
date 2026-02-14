package org.collapseloader.atlas.domain.users.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService implements InitializingBean {


    @Value("${jwt.secret}")
    private String SECRET_KEY;

    public String extractUsername(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException | IllegalArgumentException ignored) {
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        var now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        var now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username != null && username.equals(userDetails.getUsername()));
        } catch (JwtException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            final String type = extractClaim(token, c -> c.get("type", String.class));
            return "refresh".equals(type) && isTokenValid(token, userDetails);
        } catch (JwtException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes for HS256. Set jwt.secret/JWT_SECRET to a 32+ character value."
            );
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public void afterPropertiesSet() {
        if (SECRET_KEY == null || SECRET_KEY.isBlank()) {
            throw new IllegalStateException("Missing required configuration: jwt.secret (environment variable JWT_SECRET)");
        }

        String defaultPlaceholder = "default_jwt_secret_key_32bytes!x";
        if (SECRET_KEY.equals(defaultPlaceholder) || SECRET_KEY.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Invalid jwt.secret: must be at least 32 bytes and not the default placeholder. Set JWT_SECRET environment variable to a strong secret.");
        }
    }
}
