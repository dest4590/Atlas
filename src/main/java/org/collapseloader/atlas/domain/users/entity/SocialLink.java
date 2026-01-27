package org.collapseloader.atlas.domain.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "social_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "social_links_profile_platform_unique", columnNames = {"profile_id", "platform"})
        },
        indexes = {
                @Index(name = "social_links_profile_idx", columnList = "profile_id"),
                @Index(name = "social_links_platform_idx", columnList = "platform")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialPlatform platform;

    @Column(nullable = false, length = 120)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private UserProfile profile;
}
