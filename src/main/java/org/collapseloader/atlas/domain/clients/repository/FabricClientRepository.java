package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.fabric.FabricClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FabricClientRepository extends JpaRepository<FabricClient, Long> {
    @EntityGraph(attributePaths = "dependencies")
    @NullMarked
    List<FabricClient> findAllByType(ClientType type);

    @EntityGraph(attributePaths = "dependencies")
    @NullMarked
    Optional<FabricClient> findByIdAndType(Long id, ClientType type);
}
