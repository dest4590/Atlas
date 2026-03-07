package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.clients.entity.Client;

@Entity
@Table(name = "analytics_clients", indexes = {
        @Index(name = "idx_analytics_clients_launch_ts", columnList = "launch_timestamp")
})
@Data
public class AnalyticsClientRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "launch_timestamp", nullable = false)
    private Long launchTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;
}
