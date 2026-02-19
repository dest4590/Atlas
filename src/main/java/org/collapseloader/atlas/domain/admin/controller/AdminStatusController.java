package org.collapseloader.atlas.domain.admin.controller;

import org.collapseloader.atlas.AtlasApplication;
import org.collapseloader.atlas.domain.admin.dto.AdminServerStatusResponse;
import org.collapseloader.atlas.domain.admin.dto.AdminSubsystemStatusResponse;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.service.WebSocketSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminStatusController {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Environment environment;
    private final WebSocketSessionService webSocketSessionService;
    private final Path uploadRoot;

    public AdminStatusController(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            Environment environment,
            WebSocketSessionService webSocketSessionService,
            @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.webSocketSessionService = webSocketSessionService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AdminServerStatusResponse>> getStatus() {
        Instant now = Instant.now();
        Instant startedAt = Instant
                .ofEpochMilli(java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
        long uptimeSeconds = Duration.between(startedAt, now).getSeconds();
        Map<String, AdminSubsystemStatusResponse> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());
        checks.put("storage", checkStorage());
        checks.put("jvm", checkJvm());
        checks.put("system", checkSystem());
        checks.put("users", checkUsers());

        AdminServerStatusResponse payload = new AdminServerStatusResponse(
                environment.getProperty("spring.application.name", "Atlas"),
                "Operational",
                resolveVersion(),
                resolveEnvironment(),
                now,
                startedAt,
                uptimeSeconds,
                checks);
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

    private AdminSubsystemStatusResponse checkDatabase() {
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
            return new AdminSubsystemStatusResponse("UP", "Database reachable", info);
        } catch (Exception e) {
            return new AdminSubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private AdminSubsystemStatusResponse checkRedis() {
        Map<String, Object> info = new HashMap<>();
        info.put("host", environment.getProperty("spring.data.redis.host", "unknown"));
        info.put("port", environment.getProperty("spring.data.redis.port", "unknown"));
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return new AdminSubsystemStatusResponse("DOWN", "Redis connection factory not configured", info);
            }
            try (var connection = connectionFactory.getConnection()) {
                long start = System.nanoTime();
                String response = connection.ping();
                long latencyMs = (System.nanoTime() - start) / 1_000_000;
                info.put("latency_ms", latencyMs);
                info.put("ping_response", response);
                boolean pong = response != null && !response.isBlank();
                return new AdminSubsystemStatusResponse(pong ? "UP" : "DEGRADED",
                        pong ? "Ping " + response : "Ping returned empty", info);
            }
        } catch (Exception e) {
            return new AdminSubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private AdminSubsystemStatusResponse checkStorage() {
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
                return new AdminSubsystemStatusResponse("UP", "Upload root ready at " + uploadRoot, info);
            }
            return new AdminSubsystemStatusResponse("DEGRADED", "Upload root not fully accessible at " + uploadRoot,
                    info);
        } catch (Exception e) {
            return new AdminSubsystemStatusResponse("DOWN", describeError(e), info);
        }
    }

    private AdminSubsystemStatusResponse checkJvm() {
        Map<String, Object> info = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        info.put("max_memory", runtime.maxMemory());
        info.put("total_memory", runtime.totalMemory());
        info.put("free_memory", runtime.freeMemory());
        info.put("used_memory", runtime.totalMemory() - runtime.freeMemory());
        info.put("heap_usage", memoryMXBean.getHeapMemoryUsage().toString());
        info.put("non_heap_usage", memoryMXBean.getNonHeapMemoryUsage().toString());
        info.put("thread_count", ManagementFactory.getThreadMXBean().getThreadCount());
        info.put("peak_thread_count", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        info.put("java_version", System.getProperty("java.version"));
        info.put("java_vendor", System.getProperty("java.vendor"));
        return new AdminSubsystemStatusResponse("UP", "JVM Healthy", info);
    }

    private AdminSubsystemStatusResponse checkSystem() {
        Map<String, Object> info = new LinkedHashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.put("os_name", osBean.getName());
        info.put("os_version", osBean.getVersion());
        info.put("os_arch", osBean.getArch());
        info.put("available_processors", osBean.getAvailableProcessors());
        info.put("system_load_average", osBean.getSystemLoadAverage());
        return new AdminSubsystemStatusResponse("UP", "System Healthy", info);
    }

    private AdminSubsystemStatusResponse checkUsers() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("online_users", webSocketSessionService.getUserCount());
        info.put("online_guests", webSocketSessionService.getGuestCount());
        info.put("total_online", webSocketSessionService.getTotalCount());
        return new AdminSubsystemStatusResponse("UP", webSocketSessionService.getTotalCount() + " users online", info);
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
