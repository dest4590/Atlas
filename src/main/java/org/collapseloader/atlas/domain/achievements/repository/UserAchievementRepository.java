package org.collapseloader.atlas.domain.achievements.repository;

import org.collapseloader.atlas.domain.achievements.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    @Modifying
    @Query("delete from UserAchievement ua where ua.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserAchievement> findByUserId(Long userId);

    boolean existsByUserIdAndAchievementKey(Long userId, String achievementKey);

    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.user.id = :userId")
    List<UserAchievement> findAllWithAchievementByUserId(Long userId);

    long countByAchievementId(Long achievementId);

    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);
}
