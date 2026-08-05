package com.skala.shopping.member.internal;

import com.skala.shopping.member.internal.domain.MemberAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MemberAddressRepository extends JpaRepository<MemberAddress, UUID> {

    List<MemberAddress> findAllByMemberIdOrderByDefaultAddressDescCreatedAtAscIdAsc(UUID memberId);

    Optional<MemberAddress> findByIdAndMemberId(UUID addressId, UUID memberId);

    long countByMemberId(UUID memberId);

    boolean existsByMemberIdAndAddressNameIgnoreCaseAndIdNot(
            UUID memberId,
            String addressName,
            UUID addressId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update MemberAddress address
            set address.defaultAddress = false
            where address.memberId = :memberId
              and address.defaultAddress = true
            """)
    int clearDefaultAddress(@Param("memberId") UUID memberId);
}
