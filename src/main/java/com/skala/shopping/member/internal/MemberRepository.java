package com.skala.shopping.member.internal;

import com.skala.shopping.member.internal.domain.Member;
import com.skala.shopping.member.internal.domain.MemberStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByCustomerId(String customerId);

    Optional<Member> findByCustomerIdAndNameAndStatus(
            String customerId,
            String name,
            MemberStatus status
    );

    boolean existsByCustomerId(String customerId);
}
