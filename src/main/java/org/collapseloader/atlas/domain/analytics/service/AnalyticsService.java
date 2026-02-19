package org.collapseloader.atlas.domain.analytics.service;

import org.collapseloader.atlas.domain.analytics.dto.StatisticsResponse;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.collapseloader.atlas.domain.analytics.entity.OnlineUserSnapshot;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsCounterRepository;
import org.collapseloader.atlas.domain.analytics.repository.OnlineUserSnapshotRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.service.WebSocketSessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        AnalyticsCounter counter = counterRepository.findByKey(LOADER_LAUNCHES_KEY)
                .orElseGet(() -> {
                    AnalyticsCounter created = new AnalyticsCounter();
                    created.setKey(LOADER_LAUNCHES_KEY);
                    created.setValue(0);
                    return created;
                });
        counter.setValue(counter.getValue() + 1);
        return counterRepository.save(counter).getValue();
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
}
