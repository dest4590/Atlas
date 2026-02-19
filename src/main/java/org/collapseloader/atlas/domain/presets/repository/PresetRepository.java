package org.collapseloader.atlas.domain.presets.repository;

import org.collapseloader.atlas.domain.presets.entity.Preset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PresetRepository extends JpaRepository<Preset, Long> {
    void deleteAllByOwnerId(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByIsPublicTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByIsPublicTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    Page<Preset> findByOwnerIdAndIsPublicTrue(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("select p from Preset p where p.isPublic = true or (p.owner.id = :ownerId)")
    Page<Preset> findAllVisibleToUser(@Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("select p from Preset p where (p.isPublic = true or p.owner.id = :ownerId) and lower(p.name) like lower(concat('%', :name, '%'))")
    Page<Preset> findVisibleToUserByName(@Param("ownerId") Long ownerId, @Param("name") String name, Pageable pageable);

    @Query("""
            select p from Preset p
            join fetch p.owner o
            left join fetch o.profile
            where p.id = :id
            """)
    Optional<Preset> findWithOwnerById(@Param("id") Long id);
}
