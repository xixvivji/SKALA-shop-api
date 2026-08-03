package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.internal.domain.AuthAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
