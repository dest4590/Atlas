package org.collapseloader.atlas.domain.analytics.service;

import com.google.common.net.InternetDomainName;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.analytics.dto.response.AdminAnalyticsServerRecordResponse;
import org.collapseloader.atlas.domain.analytics.entity.AnalyticsServerRecord;
import org.collapseloader.atlas.domain.analytics.repository.AnalyticsServerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AnalyticsServerService {
    private final AnalyticsServerRepository serverRepository;

    public AnalyticsServerService(AnalyticsServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Transactional
    public void recordServerJoin(String domain) {
        String trimmedDomain = domain.trim().toLowerCase();

        InternetDomainName internetDomainName = InternetDomainName.from(trimmedDomain).topPrivateDomain();
        String domainName = internetDomainName.toString();

        int updated = serverRepository.incrementJoinCountByDomain(domainName);
        if (updated > 0) {
            return;
        }

        try {
            AnalyticsServerRecord server = new AnalyticsServerRecord();
            server.setDomain(domainName);
            server.setJoinCount(1L);
            serverRepository.save(server);
        } catch (DataIntegrityViolationException ex) {
            serverRepository.incrementJoinCountByDomain(domainName);
        }
    }

    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deleteOldServers() {
        log.info("Deleting old servers with joinCount < 5");

        // remove servers with joinCount < 5 every 24 hours
        serverRepository.deleteAllByJoinCountLessThan(5L);
    }

    @Transactional(readOnly = true)
    public List<AdminAnalyticsServerRecordResponse> getServerRecords() {
        return serverRepository.findAllByOrderByJoinCountDesc()
                .stream()
                .map(record -> new AdminAnalyticsServerRecordResponse(
                        record.getId(),
                        record.getDomain(),
                        record.getJoinCount()))
                .toList();
    }
}
