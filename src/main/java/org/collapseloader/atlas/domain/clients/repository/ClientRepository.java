package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findAllByType(ClientType type);

    @Query("select coalesce(sum(c.launches), 0) from Client c")
    long sumLaunches();

    @Query("select coalesce(sum(c.downloads), 0) from Client c")
    long sumDownloads();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE clients SET type = :type WHERE id = :id", nativeQuery = true)
    void updateClientType(Long id, String type);
}
