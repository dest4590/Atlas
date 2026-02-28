package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "analytics_servers")
@Data
public class AnalyticsServerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(nullable = false)
    private Long joinCount = 0L;
}
