package org.collapseloader.atlas.domain.clients.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Vanilla")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

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
    private List<ClientComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClientRating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClientScreenshot> screenshots = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void autoCalculateMetadata() {
        if (this.filename == null || this.filename.isBlank()) {
            String prefix = "";
            if (this.type == ClientType.FABRIC) {
                prefix = "clients/fabric/jars/";
            } else if (this.type == ClientType.FORGE) {
                prefix = "clients/forge/jars/";
            } else {
                prefix = "clients/vanilla/jars/";
            }
            this.filename = prefix + this.name + ".jar";
        }
    }
}
