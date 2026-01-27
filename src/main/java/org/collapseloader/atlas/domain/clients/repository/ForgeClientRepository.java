package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.forge.ForgeClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForgeClientRepository extends JpaRepository<ForgeClient, Long> {
    List<ForgeClient> findAllByType(ClientType type);

    Optional<ForgeClient> findByIdAndType(Long id, ClientType type);
}
