package com.skala.shopping.wallet.internal;

import com.skala.shopping.wallet.internal.domain.PointAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PointAccountRepository extends JpaRepository<PointAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from PointAccount account where account.memberId = :memberId")
    Optional<PointAccount> findByMemberIdForUpdate(@Param("memberId") UUID memberId);
}
