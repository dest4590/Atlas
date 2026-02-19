package org.collapseloader.atlas.domain.friends.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "friend_requests",
        indexes = {
                @Index(name = "friend_requests_requester_idx", columnList = "requester_id"),
                @Index(name = "friend_requests_addressee_idx", columnList = "addressee_id"),
                @Index(name = "friend_requests_status_idx", columnList = "status"),
                @Index(name = "friend_requests_created_idx", columnList = "created_at"),
                @Index(name = "friend_requests_updated_idx", columnList = "updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "friend_requests_pair_uniq", columnNames = {"requester_id", "addressee_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    private User blockedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
