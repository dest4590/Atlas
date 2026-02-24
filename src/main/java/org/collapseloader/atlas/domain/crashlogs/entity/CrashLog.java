package org.collapseloader.atlas.domain.crashlogs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "crash_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrashLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "username_snapshot", nullable = false)
    private String usernameSnapshot;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_version")
    private String clientVersion;

    @Column(name = "crash_type", nullable = false)
    private String crashType;

    @Column(name = "loader_version")
    private String loaderVersion;

    @Column(name = "os_name")
    private String osName;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "log_content", columnDefinition = "TEXT", nullable = false)
    private String logContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
