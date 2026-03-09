package org.collapseloader.atlas.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestTrackingFilter extends OncePerRequestFilter {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeRequests = new AtomicInteger();
    private final long slowRequestThresholdMs;

    public RequestTrackingFilter(MeterRegistry meterRegistry,
                                 @Value("${atlas.monitoring.request-slow-threshold-ms:1000}") long slowRequestThresholdMs) {
        this.meterRegistry = meterRegistry;
        this.slowRequestThresholdMs = slowRequestThresholdMs;

        Gauge.builder("atlas.http.server.requests.active", activeRequests, AtomicInteger::get)
                .description("Current in-flight Atlas HTTP requests")
                .register(meterRegistry);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator") || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        activeRequests.incrementAndGet();

        try {
            filterChain.doFilter(request, response);
        } finally {
            activeRequests.decrementAndGet();

            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            String route = resolveRoute(request);
            String status = Integer.toString(response.getStatus());
            String outcome = resolveOutcome(response.getStatus());
            Tags tags = Tags.of(
                    "method", request.getMethod(),
                    "route", route,
                    "status", status,
                    "outcome", outcome);

            meterRegistry.counter("atlas.http.server.requests", tags).increment();
            meterRegistry.timer("atlas.http.server.request.duration", tags).record(duration);

            if (duration.toMillis() >= slowRequestThresholdMs) {
                meterRegistry.counter("atlas.http.server.requests.slow", Tags.of(
                        "method", request.getMethod(),
                        "route", route,
                        "outcome", outcome)).increment();
            }
        }
    }

    private String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String value && !value.isBlank()) {
            return value;
        }
        return "UNMAPPED";
    }

    private String resolveOutcome(int statusCode) {
        if (statusCode >= 500) {
            return "SERVER_ERROR";
        }
        if (statusCode >= 400) {
            return "CLIENT_ERROR";
        }
        if (statusCode >= 300) {
            return "REDIRECTION";
        }
        if (statusCode >= 200) {
            return "SUCCESS";
        }
        return "UNKNOWN";
    }
}