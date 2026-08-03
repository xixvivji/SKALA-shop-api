package com.skala.shopping.member.internal;

import com.skala.shopping.member.internal.domain.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);
}
