package org.collapseloader.atlas.domain.analytics.service;

import org.collapseloader.atlas.domain.analytics.dto.response.StatisticsResponse;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsCounter;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsCounterRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private static final String LOADER_LAUNCHES_KEY = "loader_launches";

    private final AnalyticsCounterRepository counterRepository;
    private final ClientRepository clientRepository;

    public AnalyticsService(
            AnalyticsCounterRepository counterRepository,
            ClientRepository clientRepository) {
        this.counterRepository = counterRepository;
        this.clientRepository = clientRepository;
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
}
