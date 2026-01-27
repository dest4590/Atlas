package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientScreenshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientScreenshotRepository extends JpaRepository<ClientScreenshot, Long> {
    List<ClientScreenshot> findAllByClientIdOrderBySortOrderAsc(Long clientId);
}