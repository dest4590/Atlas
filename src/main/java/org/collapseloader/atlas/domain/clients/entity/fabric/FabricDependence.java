package org.collapseloader.atlas.domain.clients.entity.fabric;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.util.CdnMetadataUtil;

@Entity
@Table(name = "fabric_dependences", uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "name"}))
@Data
public class FabricDependence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private FabricClient client;

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

        if (this.md5Hash == null || this.md5Hash.isBlank() || this.size == 0) {
            calculateMetadataFromCdn();
        }
    }

    private void calculateMetadataFromCdn() {
        String cdnUrl = "https://cdn.collapseloader.org/fabric/deps/" + this.name + ".jar";
        CdnMetadataUtil.CdnMetadata metadata = CdnMetadataUtil.calculateMetadata(cdnUrl);
        if (metadata != null) {
            if (this.md5Hash == null || this.md5Hash.isBlank()) {
                this.md5Hash = metadata.md5();
            }
            if (this.size == 0) {
                this.size = metadata.sizeMb();
            }
        }
    }
}