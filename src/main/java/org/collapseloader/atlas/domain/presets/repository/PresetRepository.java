package org.collapseloader.atlas.domain.presets.repository;

import org.collapseloader.atlas.domain.presets.entity.Preset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PresetRepository extends JpaRepository<Preset, Long> {
    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByIsPublicTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByIsPublicTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByOwnerIdAndIsPublicTrue(Long ownerId, Pageable pageable);

    @Query("""
            select p from Preset p
            join fetch p.owner o
            left join fetch o.profile
            where p.id = :id
            """)
    Optional<Preset> findWithOwnerById(Long id);
}
