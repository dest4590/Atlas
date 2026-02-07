package org.collapseloader.atlas.domain.clients.entity.forge;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "forge_dependences", uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "name"}))
@Data
public class ForgeDependence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ForgeClient client;

    private String name;

    @Column(name = "md5_hash")
    private String md5Hash;

    private long size;

    @PrePersist
    @PreUpdate
    public void autoCalculateMetadata() {
        if (this.name != null) {
            this.name = this.name.trim();
            if (this.name.endsWith(".jar")) {
                this.name = this.name.substring(0, this.name.length() - 4);
            }
        }
    }
}
