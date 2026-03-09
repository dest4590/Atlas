package org.collapseloader.atlas.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InfrastructureMetricsCollector {
    private static final double NO_VALUE = Double.NaN;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Path uploadRoot;
    private final long databaseSpikeThresholdMs;
    private final long redisSpikeThresholdMs;

    private final AtomicInteger databaseStatus = new AtomicInteger();
    private final AtomicLong databaseLatencyMs = new AtomicLong(-1);
    private final AtomicLong databaseLastSuccessEpochSeconds = new AtomicLong();
    private final AtomicInteger databaseSpike = new AtomicInteger();

    private final AtomicInteger redisStatus = new AtomicInteger();
    private final AtomicLong redisLatencyMs = new AtomicLong(-1);
    private final AtomicLong redisLastSuccessEpochSeconds = new AtomicLong();
    private final AtomicInteger redisSpike = new AtomicInteger();

    private final AtomicInteger storageStatus = new AtomicInteger();
    private final AtomicLong storageUsableBytes = new AtomicLong(-1);
    private final AtomicLong storageTotalBytes = new AtomicLong(-1);
    private final AtomicLong storageLastCheckedEpochSeconds = new AtomicLong();

    private final Counter databaseCheckFailures;
    private final Counter databaseChecks;
    private final Counter databasePingSpikes;
    private final Counter redisCheckFailures;
    private final Counter redisChecks;
    private final Counter redisPingSpikes;
    private final Counter storageCheckFailures;
    private final Counter storageChecks;

    public InfrastructureMetricsCollector(JdbcTemplate jdbcTemplate,
                                          StringRedisTemplate redisTemplate,
                                          MeterRegistry meterRegistry,
                                          @Value("${atlas.storage.upload-dir:uploads}") String uploadDir,
                                          @Value("${atlas.monitoring.database.latency-spike-threshold-ms:250}") long databaseSpikeThresholdMs,
                                          @Value("${atlas.monitoring.redis.latency-spike-threshold-ms:100}") long redisSpikeThresholdMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.databaseSpikeThresholdMs = databaseSpikeThresholdMs;
        this.redisSpikeThresholdMs = redisSpikeThresholdMs;

        registerDependencyMeters(meterRegistry, "database", databaseStatus, databaseLatencyMs,
                databaseLastSuccessEpochSeconds, databaseSpike);
        registerDependencyMeters(meterRegistry, "redis", redisStatus, redisLatencyMs,
                redisLastSuccessEpochSeconds, redisSpike);

        Gauge.builder("atlas.storage.status", storageStatus, AtomicInteger::get)
                .description("Storage accessibility status where 1 means healthy")
                .register(meterRegistry);
        Gauge.builder("atlas.storage.usable.bytes", storageUsableBytes, this::asDoubleBytes)
                .description("Usable bytes in Atlas upload storage")
                .baseUnit("bytes")
                .register(meterRegistry);
        Gauge.builder("atlas.storage.total.bytes", storageTotalBytes, this::asDoubleBytes)
                .description("Total bytes in Atlas upload storage")
                .baseUnit("bytes")
                .register(meterRegistry);
        Gauge.builder("atlas.storage.last.checked.epoch", storageLastCheckedEpochSeconds, AtomicLong::doubleValue)
                .description("Unix epoch seconds of the most recent storage probe")
                .baseUnit("seconds")
                .register(meterRegistry);

        this.databaseChecks = Counter.builder("atlas.infrastructure.checks")
                .tag("dependency", "database")
                .register(meterRegistry);
        this.databaseCheckFailures = Counter.builder("atlas.infrastructure.check.failures")
                .tag("dependency", "database")
                .register(meterRegistry);
        this.databasePingSpikes = Counter.builder("atlas.infrastructure.ping.spikes")
                .tag("dependency", "database")
                .register(meterRegistry);

        this.redisChecks = Counter.builder("atlas.infrastructure.checks")
                .tag("dependency", "redis")
                .register(meterRegistry);
        this.redisCheckFailures = Counter.builder("atlas.infrastructure.check.failures")
                .tag("dependency", "redis")
                .register(meterRegistry);
        this.redisPingSpikes = Counter.builder("atlas.infrastructure.ping.spikes")
                .tag("dependency", "redis")
                .register(meterRegistry);

        this.storageChecks = Counter.builder("atlas.infrastructure.checks")
                .tag("dependency", "storage")
                .register(meterRegistry);
        this.storageCheckFailures = Counter.builder("atlas.infrastructure.check.failures")
                .tag("dependency", "storage")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${atlas.monitoring.infrastructure-check-interval-ms:15000}")
    public void collectMetrics() {
        collectDatabaseMetrics();
        collectRedisMetrics();
        collectStorageMetrics();
    }

    private void collectDatabaseMetrics() {
        databaseChecks.increment();
        long startedAt = System.nanoTime();

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latencyMs = nanosToMillis(startedAt);
            databaseStatus.set(1);
            databaseLatencyMs.set(latencyMs);
            databaseLastSuccessEpochSeconds.set(Instant.now().getEpochSecond());

            boolean spike = latencyMs >= databaseSpikeThresholdMs;
            databaseSpike.set(spike ? 1 : 0);
            if (spike) {
                databasePingSpikes.increment();
            }
        } catch (Exception ignored) {
            databaseStatus.set(0);
            databaseLatencyMs.set(-1);
            databaseSpike.set(0);
            databaseCheckFailures.increment();
        }
    }

    private void collectRedisMetrics() {
        redisChecks.increment();

        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                redisStatus.set(0);
                redisLatencyMs.set(-1);
                redisSpike.set(0);
                redisCheckFailures.increment();
                return;
            }

            long startedAt = System.nanoTime();
            try (var connection = connectionFactory.getConnection()) {
                String response = connection.ping();
                long latencyMs = nanosToMillis(startedAt);
                boolean healthy = response != null && !response.isBlank();

                redisStatus.set(healthy ? 1 : 0);
                redisLatencyMs.set(healthy ? latencyMs : -1);
                redisLastSuccessEpochSeconds.set(healthy ? Instant.now().getEpochSecond() : 0);

                boolean spike = healthy && latencyMs >= redisSpikeThresholdMs;
                redisSpike.set(spike ? 1 : 0);
                if (spike) {
                    redisPingSpikes.increment();
                }
                if (!healthy) {
                    redisCheckFailures.increment();
                }
            }
        } catch (Exception ignored) {
            redisStatus.set(0);
            redisLatencyMs.set(-1);
            redisSpike.set(0);
            redisCheckFailures.increment();
        }
    }

    private void collectStorageMetrics() {
        storageChecks.increment();
        storageLastCheckedEpochSeconds.set(Instant.now().getEpochSecond());

        try {
            Files.createDirectories(uploadRoot);
            boolean healthy = Files.isReadable(uploadRoot) && Files.isWritable(uploadRoot);
            storageStatus.set(healthy ? 1 : 0);

            var fileStore = Files.getFileStore(uploadRoot);
            storageUsableBytes.set(fileStore.getUsableSpace());
            storageTotalBytes.set(fileStore.getTotalSpace());

            if (!healthy) {
                storageCheckFailures.increment();
            }
        } catch (Exception ignored) {
            storageStatus.set(0);
            storageUsableBytes.set(-1);
            storageTotalBytes.set(-1);
            storageCheckFailures.increment();
        }
    }

    private void registerDependencyMeters(MeterRegistry meterRegistry,
                                          String dependency,
                                          AtomicInteger status,
                                          AtomicLong latencyMs,
                                          AtomicLong lastSuccessEpochSeconds,
                                          AtomicInteger spike) {
        Gauge.builder("atlas.infrastructure.status", status, AtomicInteger::get)
                .tag("dependency", dependency)
                .description("Dependency status where 1 means healthy")
                .register(meterRegistry);
        Gauge.builder("atlas.infrastructure.ping.latency", latencyMs, this::asLatency)
                .tag("dependency", dependency)
                .description("Dependency ping latency in milliseconds")
                .baseUnit("milliseconds")
                .register(meterRegistry);
        Gauge.builder("atlas.infrastructure.last.success.epoch", lastSuccessEpochSeconds, AtomicLong::doubleValue)
                .tag("dependency", dependency)
                .description("Unix epoch seconds of the most recent successful dependency probe")
                .baseUnit("seconds")
                .register(meterRegistry);
        Gauge.builder("atlas.infrastructure.ping.spike", spike, AtomicInteger::get)
                .tag("dependency", dependency)
                .description("Whether the latest dependency ping exceeded the configured latency threshold")
                .register(meterRegistry);
    }

    private double asLatency(AtomicLong value) {
        return value.get() >= 0 ? value.doubleValue() : NO_VALUE;
    }

    private double asDoubleBytes(AtomicLong value) {
        return value.get() >= 0 ? value.doubleValue() : NO_VALUE;
    }

    private long nanosToMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}