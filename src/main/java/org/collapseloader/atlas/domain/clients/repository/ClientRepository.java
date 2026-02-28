package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByType(ClientType type);

    @Query("select coalesce(sum(c.launches), 0) from Client c")
    long sumLaunches();

    @Query("select coalesce(sum(c.downloads), 0) from Client c")
    long sumDownloads();

    @Modifying
    @Transactional
    @Query(value = "UPDATE clients SET type = :type WHERE id = :id", nativeQuery = true)
    void updateClientType(Long id, String type);

    @Modifying
    @Query("UPDATE Client c SET c.downloads = c.downloads + 1 WHERE c.id = :id")
    int incrementDownloads(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Client c SET c.launches = c.launches + 1 WHERE c.id = :id")
    int incrementLaunches(@Param("id") Long id);

    Optional<Client> findByName(String name);
}
