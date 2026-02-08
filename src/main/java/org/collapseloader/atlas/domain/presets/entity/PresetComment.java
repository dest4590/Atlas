package org.collapseloader.atlas.domain.presets.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "preset_comments", indexes = {
        @Index(name = "preset_comments_preset_idx", columnList = "preset_id"),
        @Index(name = "preset_comments_author_idx", columnList = "user_id")
})
@Data
public class PresetComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preset_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Preset preset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
