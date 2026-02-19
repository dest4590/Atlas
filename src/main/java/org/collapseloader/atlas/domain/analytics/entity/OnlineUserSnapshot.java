package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "online_user_snapshots")
@Data
public class OnlineUserSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "user_count", nullable = false)
    private int userCount;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;
}
