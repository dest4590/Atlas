package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRatingRepository extends JpaRepository<ClientRating, Long> {
    @Modifying
    @Query("delete from ClientRating cr where cr.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.rating) FROM ClientRating r WHERE r.client.id = :clientId")
    Double getAverageRating(Long clientId);

    @Query("SELECT COUNT(r) FROM ClientRating r WHERE r.client.id = :clientId")
    Integer getRatingCount(Long clientId);

    Optional<ClientRating> findByClientIdAndUserId(Long clientId, Long userId);

    void deleteByClientIdAndUserId(Long clientId, Long userId);
}
