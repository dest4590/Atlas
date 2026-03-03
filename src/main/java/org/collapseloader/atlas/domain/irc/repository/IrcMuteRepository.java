package org.collapseloader.atlas.domain.irc.repository;

import org.collapseloader.atlas.domain.irc.entity.IrcMute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IrcMuteRepository extends JpaRepository<IrcMute, Long> {
    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
