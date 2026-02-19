package org.collapseloader.atlas.domain.presets.repository;

import org.collapseloader.atlas.domain.presets.entity.PresetLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface PresetLikeRepository extends JpaRepository<PresetLike, Long> {
    void deleteAllByUserId(Long userId);

    boolean existsByPresetIdAndUserId(Long presetId, Long userId);

    Optional<PresetLike> findByPresetIdAndUserId(Long presetId, Long userId);

    void deleteByPresetIdAndUserId(Long presetId, Long userId);

    long countByPresetId(Long presetId);

    @Query("select pl.preset.id from PresetLike pl where pl.user.id = :userId")
    Set<Long> findPresetIdsLikedByUser(Long userId);
}
