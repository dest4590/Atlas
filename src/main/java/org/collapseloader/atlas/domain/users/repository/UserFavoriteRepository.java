package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    @Modifying
    @Query("delete from UserFavorite uf where uf.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserFavorite> findByUserId(Long userId);

    Optional<UserFavorite> findByIdAndUserId(Long id, Long userId);
}
