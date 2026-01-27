package org.collapseloader.atlas.domain.clients.entity.fabric;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.collapseloader.atlas.domain.clients.entity.Client;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("FABRIC")
@Data
@EqualsAndHashCode(callSuper = true)
public class FabricClient extends Client {
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FabricDependence> dependencies = new ArrayList<>();
}
