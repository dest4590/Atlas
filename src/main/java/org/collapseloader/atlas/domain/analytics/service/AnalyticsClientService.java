package org.collapseloader.atlas.domain.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsClientRecord;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsClientRepostiory;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
