package org.collapseloader.atlas.domain.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "user_profiles_user_idx", columnList = "user_id"),
        @Index(name = "user_profiles_nickname_idx", columnList = "nickname"),
        @Index(name = "user_profiles_created_idx", columnList = "created_at"),
        @Index(name = "user_profiles_updated_idx", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @Column(length = 32)
    private String nickname;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProfileRole role = ProfileRole.USER;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<SocialLink> socialLinks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "total_playtime_seconds", nullable = false)
    @Builder.Default
    private long totalPlaytimeSeconds = 0L;

    @Column(name = "launches_count", nullable = false)
    @Builder.Default
    private long launchesCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_client_id")
    @ToString.Exclude
    private Client favoriteClient;

    private static String normalizeNickname(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getProfile() != this) {
            user.setProfile(this);
        }
    }

    @PrePersist
    @PreUpdate
    private void syncSocialLinks() {
        nickname = normalizeNickname(nickname);

        if (socialLinks == null) {
            socialLinks = new ArrayList<>();
            return;
        }
        for (var link : socialLinks) {
            if (link != null) {
                link.setProfile(this);
            }
        }
    }

    @JsonIgnore
    public String getAvatarUrl() {
        if (avatarPath == null || avatarPath.isBlank()) {
            return null;
        }
        String url = avatarPath.startsWith("/") ? avatarPath : "/" + avatarPath;
        if (updatedAt != null) {
            url = url + (url.contains("?") ? "&" : "?") + "v=" + updatedAt.getEpochSecond();
        }
        return url;
    }
}
