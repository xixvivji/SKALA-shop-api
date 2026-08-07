package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.internal.domain.AuthAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByLoginId(String loginId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AuthAccount account where account.id = :accountId")
    Optional<AuthAccount> findByIdForPasswordChange(@Param("accountId") UUID accountId);

    boolean existsByLoginId(String loginId);
}
