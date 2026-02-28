package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.clients.entity.Client;

@Entity
@Table(name = "analytics_clients")
@Data
public class AnalyticsClientRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Long launchTimestamp;
}
