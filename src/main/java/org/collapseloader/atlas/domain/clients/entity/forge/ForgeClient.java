package org.collapseloader.atlas.domain.clients.entity.forge;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.collapseloader.atlas.domain.clients.entity.Client;

@Entity
@DiscriminatorValue("FORGE")
@Data
@EqualsAndHashCode(callSuper = true)
public class ForgeClient extends Client {
}
