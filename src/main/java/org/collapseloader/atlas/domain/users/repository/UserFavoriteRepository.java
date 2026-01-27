package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUserId(Long userId);

    Optional<UserFavorite> findByIdAndUserId(Long id, Long userId);
}
