package org.collapseloader.atlas.domain.clients.entity.forge;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.collapseloader.atlas.domain.clients.entity.Client;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("FORGE")
@Data
@EqualsAndHashCode(callSuper = true)
public class ForgeClient extends Client {
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ForgeDependence> dependencies = new ArrayList<>();
}
