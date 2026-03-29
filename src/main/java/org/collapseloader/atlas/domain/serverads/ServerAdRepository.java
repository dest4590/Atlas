package org.collapseloader.atlas.domain.serverads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServerAdRepository extends JpaRepository<ServerAd, Long> {
    List<ServerAd> findAllByActiveTrue();
}
