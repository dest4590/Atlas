package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.AtlasApplication;
import org.collapseloader.atlas.dto.ServerStatusResponse;
import org.collapseloader.atlas.dto.SubsystemStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin")
public class StatusController {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Environment environment;
    private final Path uploadRoot;

    public StatusController(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            Environment environment,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ServerStatusResponse>> getStatus() {
        Instant now = Instant.now();
        Instant startedAt = Instant.ofEpochMilli(java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
        long uptimeSeconds = Duration.between(startedAt, now).getSeconds();
        Map<String, SubsystemStatusResponse> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());
        checks.put("storage", checkStorage());

        ServerStatusResponse payload = new ServerStatusResponse(
                environment.getProperty("spring.application.name", "Atlas"),
                "Operational",
                resolveVersion(),
                resolveEnvironment(),
                now,
                startedAt,
                uptimeSeconds,
                checks
        );
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    private String resolveEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(",", profiles) : "default";
    }

    private String resolveVersion() {
        return Optional.ofNullable(AtlasApplication.class.getPackage().getImplementationVersion())
                .filter(version -> !version.isBlank())
                .orElseGet(() -> environment.getProperty("atlas.version", "0.0.1-SNAPSHOT"));
    }

    private SubsystemStatusResponse checkDatabase() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            String url = environment.getProperty("spring.datasource.url", "unknown");
            info.put("url", redactCredentials(url));
            long start = System.nanoTime();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            info.put("latency_ms", latencyMs);
            try {
                String version = jdbcTemplate.queryForObject("select version()", String.class);
                if (version != null) {
                    info.put("version", version);
                }
            } catch (Exception ignored) {
            }
            return new SubsystemStatusResponse("UP", "Database reachable", info);
        } catch (Exception e) {
            return new SubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private SubsystemStatusResponse checkRedis() {
        Map<String, Object> info = new HashMap<>();
        info.put("host", environment.getProperty("spring.data.redis.host", "unknown"));
        info.put("port", environment.getProperty("spring.data.redis.port", "unknown"));
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return new SubsystemStatusResponse("DOWN", "Redis connection factory not configured", info);
            }
            try (var connection = connectionFactory.getConnection()) {
                long start = System.nanoTime();
                String response = connection.ping();
                long latencyMs = (System.nanoTime() - start) / 1_000_000;
                info.put("latency_ms", latencyMs);
                info.put("ping_response", response);
                boolean pong = response != null && !response.isBlank();
                return new SubsystemStatusResponse(pong ? "UP" : "DEGRADED", pong ? "Ping " + response : "Ping returned empty", info);
            }
        } catch (Exception e) {
            return new SubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private SubsystemStatusResponse checkStorage() {
        Map<String, Object> info = new HashMap<>();
        try {
            Files.createDirectories(uploadRoot);
            boolean readable = Files.isReadable(uploadRoot);
            boolean writable = Files.isWritable(uploadRoot);
            info.put("path", uploadRoot.toString());
            try {
                var store = Files.getFileStore(uploadRoot);
                info.put("total_bytes", store.getTotalSpace());
                info.put("usable_bytes", store.getUsableSpace());
                info.put("unallocated_bytes", store.getUnallocatedSpace());
            } catch (Exception ignored) {
            }
            if (readable && writable) {
                return new SubsystemStatusResponse("UP", "Upload root ready at " + uploadRoot, info);
            }
            return new SubsystemStatusResponse("DEGRADED", "Upload root not fully accessible at " + uploadRoot, info);
        } catch (Exception e) {
            return new SubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private String describeError(Exception e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null && !message.isBlank() ? message : root.toString();
    }

    private String redactCredentials(String url) {
        if (url == null) {
            return "unknown";
        }
        return url.replaceAll("(?i)(password|pwd)=([^;&]+)", "$1=***");
    }
}
