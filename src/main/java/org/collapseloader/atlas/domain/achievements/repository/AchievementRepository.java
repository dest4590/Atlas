package org.collapseloader.atlas.domain.achievements.repository;

import org.collapseloader.atlas.domain.achievements.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByKey(String key);
}
