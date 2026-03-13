package org.collapseloader.atlas.domain.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.collapseloader.atlas.domain.clients.entity.Client;

/**
 * Analytics Client Record.
 * Contains information about a client launch event, including the client,
 * launch timestamp, and platform.
 * Fields:
 * {@link #client} - The {@link Client} associated with the launch event.
 * {@link #launchTimestamp} - The timestamp of the client launch event.
 * {@link #platform} - The {@link Platform} platform on which the client was
 * launched.
 */
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Client client;

    @Column(name = "launch_timestamp", nullable = false)
    private Long launchTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;
}
