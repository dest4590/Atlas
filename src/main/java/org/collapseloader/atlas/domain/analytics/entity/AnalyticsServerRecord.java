package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "analytics_servers", indexes = {
    @Index(name = "idx_analytics_servers_join_count", columnList = "join_count")
})
@Data
public class AnalyticsServerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "join_count", nullable = false)
    private Long joinCount = 0L;
}
