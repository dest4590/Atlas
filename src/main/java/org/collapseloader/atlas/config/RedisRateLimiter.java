package org.collapseloader.atlas.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final StringRedisTemplate redisTemplate;

    @Value("${security.rate-limit.auth.max-requests:10}")
    private int authMaxRequests;

    @Value("${security.rate-limit.auth.window-seconds:60}")
    private long authWindowSeconds;

    public RateLimitResult checkAuthLimit(String ip) {
        String key = "rate_limit:auth:" + ip;
        Long current = redisTemplate.opsForValue().increment(key);

        if (current == null) {
            return new RateLimitResult(false, authMaxRequests, authMaxRequests, authWindowSeconds);
        }

        if (current == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(authWindowSeconds));
        }

        Long ttl = redisTemplate.getExpire(key);
        long retryAfter = (ttl == null || ttl < 0) ? authWindowSeconds : ttl;

        if (current > authMaxRequests) {
            return new RateLimitResult(true, 0, authMaxRequests, retryAfter);
        }

        long remaining = Math.max(0, authMaxRequests - current);
        return new RateLimitResult(false, remaining, authMaxRequests, retryAfter);
    }

    public record RateLimitResult(boolean blocked, long remaining, long limit, long retryAfterSeconds) {
    }
}
