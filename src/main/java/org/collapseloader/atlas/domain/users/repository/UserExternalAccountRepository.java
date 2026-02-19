package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.UserExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserExternalAccountRepository extends JpaRepository<UserExternalAccount, Long> {
    @Modifying
    @Query("delete from UserExternalAccount uea where uea.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserExternalAccount> findByUserId(Long userId);

    Optional<UserExternalAccount> findByIdAndUserId(Long id, Long userId);
}
