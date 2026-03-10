package org.collapseloader.atlas.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsClientRepostiory;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsCounterRepository;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsServerRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientCommentRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRatingRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.crashlogs.repository.CrashLogRepository;
import org.collapseloader.atlas.domain.friends.entity.FriendRequestStatus;
import org.collapseloader.atlas.domain.friends.repository.FriendRequestRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetRepository;
import org.collapseloader.atlas.domain.reports.entity.ReportStatus;
import org.collapseloader.atlas.domain.reports.repository.UserReportRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.titan.repository.FileMetadataRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Component
public class BusinessMetricsCollector {
    private static final String LOADER_LAUNCHES_KEY = "loader_launches";
    private static final double NO_VALUE = Double.NaN;

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PresetRepository presetRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserReportRepository userReportRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final CrashLogRepository crashLogRepository;
    private final AnalyticsClientRepostiory analyticsClientRepository;
    private final AnalyticsServerRepository analyticsServerRepository;
    private final AnalyticsCounterRepository analyticsCounterRepository;
    private final ClientCommentRepository clientCommentRepository;
    private final ClientRatingRepository clientRatingRepository;

    private final AtomicLong registeredUsers = new AtomicLong(-1);
    private final AtomicLong publishedClients = new AtomicLong(-1);
    private final AtomicLong savedPresets = new AtomicLong(-1);
    private final AtomicLong trackedFiles = new AtomicLong(-1);
    private final AtomicLong openReports = new AtomicLong(-1);
    private final AtomicLong pendingFriendRequests = new AtomicLong(-1);
    private final AtomicLong storedCrashLogs = new AtomicLong(-1);
    private final AtomicLong analyticsClientRecords = new AtomicLong(-1);
    private final AtomicLong analyticsServerRecords = new AtomicLong(-1);
    private final AtomicLong clientComments = new AtomicLong(-1);
    private final AtomicLong clientRatings = new AtomicLong(-1);
    private final AtomicLong clientDownloads = new AtomicLong(-1);
    private final AtomicLong clientLaunches = new AtomicLong(-1);
    private final AtomicLong loaderLaunches = new AtomicLong(-1);
    private final AtomicLong lastCollectedEpochSeconds = new AtomicLong();

    private final Counter collectionFailures;

    public BusinessMetricsCollector(UserRepository userRepository,
            ClientRepository clientRepository,
            PresetRepository presetRepository,
            FileMetadataRepository fileMetadataRepository,
            UserReportRepository userReportRepository,
            FriendRequestRepository friendRequestRepository,
            CrashLogRepository crashLogRepository,
            AnalyticsClientRepostiory analyticsClientRepository,
            AnalyticsServerRepository analyticsServerRepository,
            AnalyticsCounterRepository analyticsCounterRepository,
            ClientCommentRepository clientCommentRepository,
            ClientRatingRepository clientRatingRepository,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.presetRepository = presetRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.userReportRepository = userReportRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.crashLogRepository = crashLogRepository;
        this.analyticsClientRepository = analyticsClientRepository;
        this.analyticsServerRepository = analyticsServerRepository;
        this.analyticsCounterRepository = analyticsCounterRepository;
        this.clientCommentRepository = clientCommentRepository;
        this.clientRatingRepository = clientRatingRepository;

        registerGauge(meterRegistry, "atlas.business.users.registered", registeredUsers,
                "Registered Atlas users");
        registerGauge(meterRegistry, "atlas.business.clients.published", publishedClients,
                "Published Atlas clients");
        registerGauge(meterRegistry, "atlas.business.presets.saved", savedPresets,
                "Stored Atlas presets");
        registerGauge(meterRegistry, "atlas.business.files.tracked", trackedFiles,
                "Tracked file metadata records in Titan");
        registerGauge(meterRegistry, "atlas.business.reports.open", openReports,
                "Open Atlas moderation reports");
        registerGauge(meterRegistry, "atlas.business.friend.requests.pending", pendingFriendRequests,
                "Pending Atlas friend requests");
        registerGauge(meterRegistry, "atlas.business.crashlogs.stored", storedCrashLogs,
                "Stored Atlas crash logs");
        registerGauge(meterRegistry, "atlas.business.analytics.client.records", analyticsClientRecords,
                "Stored Atlas client analytics records");
        registerGauge(meterRegistry, "atlas.business.analytics.server.records", analyticsServerRecords,
                "Stored Atlas server analytics records");
        registerGauge(meterRegistry, "atlas.business.client.comments", clientComments,
                "Stored Atlas client comments");
        registerGauge(meterRegistry, "atlas.business.client.ratings", clientRatings,
                "Stored Atlas client ratings");
        registerGauge(meterRegistry, "atlas.business.client.downloads", clientDownloads,
                "Cumulative client downloads stored in Atlas");
        registerGauge(meterRegistry, "atlas.business.client.launches", clientLaunches,
                "Cumulative client launches stored in Atlas");
        registerGauge(meterRegistry, "atlas.business.loader.launches", loaderLaunches,
                "Cumulative loader launches tracked in Atlas analytics counters");

        Gauge.builder("atlas.business.metrics.last.collected.epoch", lastCollectedEpochSeconds, AtomicLong::doubleValue)
                .description("Unix epoch seconds of the most recent business metrics collection")
                .baseUnit("seconds")
                .register(meterRegistry);

        this.collectionFailures = Counter.builder("atlas.business.metrics.collection.failures")
                .description("Business metrics collection failures")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${atlas.monitoring.business-check-interval-ms:60000}")
    public void collectMetrics() {
        updateGauge(registeredUsers, userRepository::count);
        updateGauge(publishedClients, clientRepository::count);
        updateGauge(savedPresets, presetRepository::count);
        updateGauge(trackedFiles, fileMetadataRepository::count);
        updateGauge(openReports, () -> userReportRepository.countByStatus(ReportStatus.PENDING));
        updateGauge(pendingFriendRequests, () -> friendRequestRepository.countByStatus(FriendRequestStatus.PENDING));
        updateGauge(storedCrashLogs, crashLogRepository::count);
        updateGauge(analyticsClientRecords, analyticsClientRepository::count);
        updateGauge(analyticsServerRecords, analyticsServerRepository::count);
        updateGauge(clientComments, clientCommentRepository::count);
        updateGauge(clientRatings, clientRatingRepository::count);
        updateGauge(clientDownloads, clientRepository::sumDownloads);
        updateGauge(clientLaunches, clientRepository::sumLaunches);
        updateGauge(loaderLaunches, () -> analyticsCounterRepository.findByKey(LOADER_LAUNCHES_KEY)
                .map(AnalyticsCounter::getValue)
                .orElse(0L));
        lastCollectedEpochSeconds.set(Instant.now().getEpochSecond());
    }

    @PostConstruct
    public void collectMetricsAtStartup() {
        collectMetrics();
    }

    private void registerGauge(MeterRegistry meterRegistry,
            String name,
            AtomicLong value,
            String description) {
        Gauge.builder(name, value, this::asGaugeValue)
                .description(description)
                .register(meterRegistry);
    }

    private void updateGauge(AtomicLong target, LongSupplier supplier) {
        try {
            target.set(supplier.getAsLong());
        } catch (Exception ignored) {
            target.set(-1);
            collectionFailures.increment();
        }
    }

    private double asGaugeValue(AtomicLong value) {
        return value.get() >= 0 ? value.doubleValue() : NO_VALUE;
    }
}