package org.collapseloader.atlas.domain.irc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * IrcMute entity representing a permanent or temporary mute applied to a user on IRC.
 * <p>
 * Stores the muted user, a reason, the actor who issued the mute, the timestamp
 * when it was applied and an optional expiry time.
 */
@Entity
@Table(name = "irc_mutes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrcMute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muted_by")
    private User mutedBy;

    @CreationTimestamp
    @Column(name = "muted_at", nullable = false, updatable = false)
    private Instant mutedAt;

    private Instant expiresAt;
}
