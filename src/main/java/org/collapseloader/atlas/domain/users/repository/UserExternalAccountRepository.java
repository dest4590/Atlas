package org.collapseloader.atlas.domain.users.repository;

import org.collapseloader.atlas.domain.users.entity.UserExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserExternalAccountRepository extends JpaRepository<UserExternalAccount, Long> {
    List<UserExternalAccount> findByUserId(Long userId);

    Optional<UserExternalAccount> findByIdAndUserId(Long id, Long userId);
}
