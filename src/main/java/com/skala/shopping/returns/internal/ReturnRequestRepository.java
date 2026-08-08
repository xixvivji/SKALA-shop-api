package com.skala.shopping.returns.internal;

import com.skala.shopping.returns.internal.domain.ReturnRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    Optional<ReturnRequest> findByMemberIdAndCommandId(UUID memberId, UUID commandId);
    Page<ReturnRequest> findAllByMemberId(UUID memberId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ReturnRequest request where request.id=:id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);
}
