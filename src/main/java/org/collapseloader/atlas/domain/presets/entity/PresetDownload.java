package org.collapseloader.atlas.domain.presets.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "preset_downloads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_preset_user_download", columnNames = {"preset_id", "user_id"})
}, indexes = {
        @Index(name = "idx_preset_download_preset", columnList = "preset_id"),
        @Index(name = "idx_preset_download_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresetDownload {
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
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
