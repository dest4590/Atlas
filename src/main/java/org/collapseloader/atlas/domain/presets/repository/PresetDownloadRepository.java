package org.collapseloader.atlas.domain.presets.repository;

import org.collapseloader.atlas.domain.presets.entity.PresetDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PresetDownloadRepository extends JpaRepository<PresetDownload, Long> {
    void deleteAllByUserId(Long userId);

    boolean existsByPresetIdAndUserId(Long presetId, Long userId);
}
