package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    @Modifying
    @Query("delete from UserPreference up where up.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserPreference> findByUserId(Long userId);

    Optional<UserPreference> findByUserIdAndKey(Long userId, String key);

    void deleteByUserIdAndKey(Long userId, String key);
}
