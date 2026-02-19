package org.collapseloader.atlas.domain.presets.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "preset_likes", uniqueConstraints = @UniqueConstraint(name = "preset_like_unique", columnNames = {
        "preset_id", "user_id"}), indexes = {
        @Index(name = "preset_likes_preset_idx", columnList = "preset_id"),
        @Index(name = "preset_likes_user_idx", columnList = "user_id")
})
@Data
public class PresetLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preset_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Preset preset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
