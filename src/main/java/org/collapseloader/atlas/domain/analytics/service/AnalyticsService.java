package org.collapseloader.atlas.domain.analytics.service;

import org.collapseloader.atlas.domain.analytics.dto.response.StatisticsResponse;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.collapseloader.atlas.domain.analytics.entity.OnlineUserSnapshot;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsCounterRepository;
import org.collapseloader.atlas.domain.analytics.repository.OnlineUserSnapshotRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.service.WebSocketSessionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AnalyticsService {
    private static final String LOADER_LAUNCHES_KEY = "loader_launches";

    private final AnalyticsCounterRepository counterRepository;
    private final ClientRepository clientRepository;
    private final OnlineUserSnapshotRepository snapshotRepository;
    private final WebSocketSessionService webSocketSessionService;

    public AnalyticsService(
            AnalyticsCounterRepository counterRepository,
            ClientRepository clientRepository,
            OnlineUserSnapshotRepository snapshotRepository,
            WebSocketSessionService webSocketSessionService) {
        this.counterRepository = counterRepository;
        this.clientRepository = clientRepository;
        this.snapshotRepository = snapshotRepository;
        this.webSocketSessionService = webSocketSessionService;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void takeOnlineUserSnapshot() {
        OnlineUserSnapshot snapshot = new OnlineUserSnapshot();
        snapshot.setTimestamp(Instant.now());
        snapshot.setUserCount(webSocketSessionService.getUserCount());
        snapshot.setGuestCount(webSocketSessionService.getGuestCount());
        snapshot.setTotalCount(webSocketSessionService.getTotalCount());
        snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public List<OnlineUserSnapshot> getOnlineHistory(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return snapshotRepository.findAllByTimestampAfterOrderByTimestampAsc(since);
    }

    @Transactional
    public long incrementLoaderLaunches() {
        int updated = counterRepository.incrementValueByKey(LOADER_LAUNCHES_KEY);
        if (updated == 0) {
            try {
                AnalyticsCounter created = new AnalyticsCounter();
                created.setKey(LOADER_LAUNCHES_KEY);
                created.setValue(0);
                counterRepository.save(created);
            } catch (DataIntegrityViolationException ignored) {
            }
            counterRepository.incrementValueByKey(LOADER_LAUNCHES_KEY);
        }

        return counterRepository.findByKey(LOADER_LAUNCHES_KEY)
                .map(AnalyticsCounter::getValue)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        long totalClientLaunches = clientRepository.sumLaunches();
        long totalClientDownloads = clientRepository.sumDownloads();
        long totalLoaderLaunches = counterRepository.findByKey(LOADER_LAUNCHES_KEY)
                .map(AnalyticsCounter::getValue)
                .orElse(0L);
        return new StatisticsResponse(totalClientLaunches, totalClientDownloads, totalLoaderLaunches);
    }

    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS) // Every 24 hours
    @Transactional
    public void cleanupOldSnapshots() {
        // clean snapshots older than 7 days
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        snapshotRepository.deleteAllByTimestampBefore(cutoff);
    }
}
