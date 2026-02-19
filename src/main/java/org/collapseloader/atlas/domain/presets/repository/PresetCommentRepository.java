package org.collapseloader.atlas.domain.presets.repository;

import org.collapseloader.atlas.domain.presets.entity.PresetComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PresetCommentRepository extends JpaRepository<PresetComment, Long> {
    void deleteAllByUserId(Long userId);

    @Query("""
            select c from PresetComment c
            join fetch c.user u
            left join fetch u.profile p
            where c.preset.id = :presetId
            order by c.createdAt desc
            """)
    List<PresetComment> findByPresetIdWithAuthors(Long presetId);

    Optional<PresetComment> findByIdAndPresetId(Long commentId, Long presetId);

    long countByPresetId(Long presetId);
}
