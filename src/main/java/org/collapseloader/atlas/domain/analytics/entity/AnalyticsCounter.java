package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Just a simple counter for analytics.
 * Used for anonymous tracking of events, like "how many times the user opened the app" and etc.
 */
@Entity
@Table(name = "analytics_counters")
@Data
public class AnalyticsCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_key", unique = true, nullable = false)
    private String key;

    @Column(nullable = false)
    private long value;
}
