package org.collapseloader.atlas.domain.clients.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.collapseloader.atlas.util.CdnMetadataUtil;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Vanilla")
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Version version;

    private String filename;

    @Column(name = "md5_hash")
    private String md5Hash;

    private long size;

    @Column(name = "main_class")
    private String mainClass;

    private boolean show;

    private boolean working;

    private long launches;

    private long downloads;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, insertable = false, updatable = false)
    private ClientType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClientComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClientRating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClientScreenshot> screenshots = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void autoCalculateMetadata() {
        if (this.filename == null || this.filename.isBlank()) {
            String prefix = "";
            if (this.type == ClientType.FABRIC) {
                prefix = "fabric/";
            } else if (this.type == ClientType.FORGE) {
                prefix = "forge/";
            }
            this.filename = prefix + this.name + ".jar";
        }

        if (this.md5Hash == null || this.md5Hash.isBlank() || this.size == 0) {
            calculateMetadataFromCdn();
        }
    }

    private void calculateMetadataFromCdn() {
        String cdnUrl = "https://cdn.collapseloader.org/" + this.filename;
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
