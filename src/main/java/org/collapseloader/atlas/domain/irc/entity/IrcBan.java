package org.collapseloader.atlas.domain.irc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "irc_bans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrcBan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by")
    private User bannedBy;

    @CreationTimestamp
    @Column(name = "banned_at", nullable = false, updatable = false)
    private Instant bannedAt;

    private Instant expiresAt;
}
