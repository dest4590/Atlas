package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.forge.ForgeClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForgeClientRepository extends JpaRepository<ForgeClient, Long> {
    @EntityGraph(attributePaths = "dependencies")
    @NullMarked
    List<ForgeClient> findAllByType(ClientType type);

    @EntityGraph(attributePaths = "dependencies")
    @NullMarked
    Optional<ForgeClient> findByIdAndType(Long id, ClientType type);
}
