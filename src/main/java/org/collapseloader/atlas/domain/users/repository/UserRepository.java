package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = { "profile" })
    @Query("""
            select u from User u
            left join u.profile p
            where lower(u.username) like lower(concat('%', :query, '%'))
               or lower(p.nickname) like lower(concat('%', :query, '%'))
            """)
    List<User> searchUsers(@Param("query") String query, Pageable pageable);
}
