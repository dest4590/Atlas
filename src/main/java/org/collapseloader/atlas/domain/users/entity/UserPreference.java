package org.collapseloader.atlas.domain.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.time.Instant;

@Entity
@Table(name = "user_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "user_preferences_user_key_unique", columnNames = {"user_id", "pref_key"})
}, indexes = {
        @Index(name = "user_preferences_user_idx", columnList = "user_id"),
        @Index(name = "user_preferences_key_idx", columnList = "pref_key"),
        @Index(name = "user_preferences_updated_idx", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @ToString.Exclude
    private User user;
    @Column(name = "pref_key", length = 120, nullable = false)
    private String key;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pref_value", columnDefinition = "jsonb", nullable = false)
    private JsonNode value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    private static JsonNode minify(JsonNode source) {
        if (source == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsBytes(source));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to minify preference payload", e);
        }
    }

    public void setValue(JsonNode value) {
        this.value = minify(value);
    }
}
