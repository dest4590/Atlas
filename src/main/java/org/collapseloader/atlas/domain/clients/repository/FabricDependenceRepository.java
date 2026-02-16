package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.fabric.FabricDependence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FabricDependenceRepository extends JpaRepository<FabricDependence, Long> {
    List<FabricDependence> findAllByMd5Hash(String md5Hash);
}
