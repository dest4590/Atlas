package org.collapseloader.atlas.domain.clients.repository;

import org.collapseloader.atlas.domain.clients.entity.ClientComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientCommentRepository extends JpaRepository<ClientComment, Long> {
    int countByClientId(Long clientId);

    @Query("""
            select c from ClientComment c
            join fetch c.user u
            left join fetch u.profile p
            where c.client.id = :clientId
            order by c.createdAt desc
            """)
    List<ClientComment> findByClientIdWithAuthors(Long clientId);

    Optional<ClientComment> findByIdAndClientId(Long commentId, Long clientId);
}
