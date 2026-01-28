package org.collapseloader.atlas.domain.achievements.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_achievements", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant unlockedAt;

    public UserAchievement(User user, Achievement achievement) {
        this.user = user;
        this.achievement = achievement;
    }
}
