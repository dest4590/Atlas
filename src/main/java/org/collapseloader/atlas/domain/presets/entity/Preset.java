package org.collapseloader.atlas.domain.presets.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presets", indexes = {
        @Index(name = "presets_owner_idx", columnList = "owner_id"),
        @Index(name = "presets_public_idx", columnList = "is_public"),
        @Index(name = "presets_created_idx", columnList = "created_at")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude
    private User owner;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(length = 2048)
    private String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Embedded
    private PresetTheme theme;

    @Column(name = "likes_count", nullable = false)
    private long likesCount;

    @Column(name = "downloads_count", nullable = false)
    private long downloadsCount;

    @Column(name = "comments_count", nullable = false)
    private long commentsCount;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<PresetLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<PresetComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<PresetDownload> downloads = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void ensureTheme() {
        if (theme == null) {
            theme = new PresetTheme();
        }
    }
}
