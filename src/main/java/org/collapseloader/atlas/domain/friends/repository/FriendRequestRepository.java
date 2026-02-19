package org.collapseloader.atlas.domain.friends.repository;

import org.collapseloader.atlas.domain.friends.entity.FriendRequest;
import org.collapseloader.atlas.domain.friends.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    @Modifying
    @Query("delete from FriendRequest fr where fr.requester.id = :userId or fr.addressee.id = :userId or fr.blockedBy.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            select fr from FriendRequest fr
            where (fr.requester.id = :userId or fr.addressee.id = :userId)
            and fr.status = :status
            order by fr.updatedAt desc
            """)
    List<FriendRequest> findByStatusForUser(@Param("userId") Long userId, @Param("status") FriendRequestStatus status);

    @Query("""
            select fr from FriendRequest fr
            where fr.requester.id = :userId and fr.addressee.id = :otherUserId
            or fr.requester.id = :otherUserId and fr.addressee.id = :userId
            """)
    Optional<FriendRequest> findBetweenUsers(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Query("""
            select fr from FriendRequest fr
            join fetch fr.requester r
            left join fetch r.profile
            join fetch fr.addressee a
            left join fetch a.profile
            where fr.id = :id
            """)
    Optional<FriendRequest> findByIdWithUsers(@Param("id") Long id);

    @Query("""
            select fr from FriendRequest fr
            join fetch fr.requester r
            left join fetch r.profile
            join fetch fr.addressee a
            left join fetch a.profile
            where fr.status = :status and fr.addressee.id = :userId
            order by fr.updatedAt desc
            """)
    List<FriendRequest> findIncomingRequests(
            @Param("userId") Long userId,
            @Param("status") FriendRequestStatus status
    );

    @Query("""
            select fr from FriendRequest fr
            join fetch fr.requester r
            left join fetch r.profile
            join fetch fr.addressee a
            left join fetch a.profile
            where fr.status = :status and fr.requester.id = :userId
            order by fr.updatedAt desc
            """)
    List<FriendRequest> findOutgoingRequests(
            @Param("userId") Long userId,
            @Param("status") FriendRequestStatus status
    );

    @Query("""
            select fr from FriendRequest fr
            join fetch fr.requester r
            left join fetch r.profile
            join fetch fr.addressee a
            left join fetch a.profile
            where (fr.requester.id = :userId and fr.addressee.id in :otherUserIds)
               or (fr.addressee.id = :userId and fr.requester.id in :otherUserIds)
            """)
    List<FriendRequest> findAllBetweenUserAndOthers(
            @Param("userId") Long userId,
            @Param("otherUserIds") List<Long> otherUserIds
    );
}
