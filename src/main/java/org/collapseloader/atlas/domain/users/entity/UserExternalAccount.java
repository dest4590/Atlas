package org.collapseloader.atlas.domain.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "user_external_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "user_external_accounts_user_provider_external_unique",
                        columnNames = {"user_id", "provider", "external_id"}
                )
        },
        indexes = {
                @Index(name = "user_external_accounts_user_idx", columnList = "user_id"),
                @Index(name = "user_external_accounts_provider_idx", columnList = "provider"),
                @Index(name = "user_external_accounts_updated_idx", columnList = "updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserExternalAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @Column(name = "provider", length = 60, nullable = false)
    private String provider;

    @Column(name = "external_id", length = 160, nullable = false)
    private String externalId;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
