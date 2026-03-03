package org.collapseloader.atlas.domain.irc.repository;

import org.collapseloader.atlas.domain.irc.entity.IrcBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IrcBanRepository extends JpaRepository<IrcBan, Long> {
    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
