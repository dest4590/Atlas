package org.collapseloader.atlas.domain.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.analytics.dto.response.AdminAnalyticsClientRecordResponse;
import org.collapseloader.atlas.domain.analytics.dto.response.GrafanaClientLaunchPointResponse;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsClientRecord;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsClientRepostiory;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AnalyticsClientService {
    private final AnalyticsClientRepostiory analyticsClientRepostiory;
    private final ClientRepository clientRepository;

    public AnalyticsClientService(AnalyticsClientRepostiory analyticsClientRepostiory, ClientRepository clientRepository) {
        this.analyticsClientRepostiory = analyticsClientRepostiory;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public void recordClientLaunch(String clientName) {
        Client client = clientRepository.findByName(clientName)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientName));

        if (client != null) {
            AnalyticsClientRecord record = new AnalyticsClientRecord();

            record.setClient(client);
            record.setLaunchTimestamp(System.currentTimeMillis());

            analyticsClientRepostiory.save(record);
        }
    }

    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS) // every 12 hours
    @Transactional
    public void cleanupOldRecords() {
        log.info("Cleaning up old analytics client records");

        long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30); // 30 days ago

        // remove records older than 30 days
        analyticsClientRepostiory.deleteByLaunchTimestampBefore(cutoffTime);
    }

    @Transactional(readOnly = true)
    public List<AdminAnalyticsClientRecordResponse> getRecentClientRecords(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));

        return analyticsClientRepostiory.findTop200ByOrderByLaunchTimestampDesc()
                .stream()
                .limit(safeLimit)
                .map(record -> new AdminAnalyticsClientRecordResponse(
                        record.getId(),
                        record.getClient() != null ? record.getClient().getId() : null,
                        record.getClient() != null ? record.getClient().getName() : null,
                        record.getLaunchTimestamp()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrafanaClientLaunchPointResponse> getGrafanaLaunchSeries(Long from, Long to, Integer intervalMinutes) {
        long now = System.currentTimeMillis();
        long safeTo = to != null ? to : now;
        long safeFrom = from != null ? from : safeTo - TimeUnit.HOURS.toMillis(24);
        if (safeFrom > safeTo) {
            long temp = safeFrom;
            safeFrom = safeTo;
            safeTo = temp;
        }

        int safeIntervalMinutes = intervalMinutes != null ? intervalMinutes : 60;
        safeIntervalMinutes = Math.max(1, Math.min(safeIntervalMinutes, 1440));
        long bucketMs = TimeUnit.MINUTES.toMillis(safeIntervalMinutes);

        List<AnalyticsClientRecord> records = analyticsClientRepostiory
                .findAllByLaunchTimestampBetweenOrderByLaunchTimestampAsc(safeFrom, safeTo);

        Map<String, Long> buckets = new LinkedHashMap<>();
        for (AnalyticsClientRecord record : records) {
            if (record.getLaunchTimestamp() == null) {
                continue;
            }
            long bucketStart = (record.getLaunchTimestamp() / bucketMs) * bucketMs;
            String clientName = record.getClient() != null ? record.getClient().getName() : "unknown";
            String key = bucketStart + "|" + clientName;
            buckets.put(key, buckets.getOrDefault(key, 0L) + 1L);
        }

        List<GrafanaClientLaunchPointResponse> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            long time = Long.parseLong(parts[0]);
            String clientName = parts.length > 1 ? parts[1] : "unknown";
            points.add(new GrafanaClientLaunchPointResponse(time, clientName, entry.getValue()));
        }

        points.sort(Comparator
                .comparingLong(GrafanaClientLaunchPointResponse::time)
                .thenComparing(GrafanaClientLaunchPointResponse::client));

        return points;
    }
}
