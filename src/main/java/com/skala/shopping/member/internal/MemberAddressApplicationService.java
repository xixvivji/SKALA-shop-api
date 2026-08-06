package com.skala.shopping.member.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.member.MemberAddressView;
import com.skala.shopping.member.internal.domain.MemberAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAddressApplicationService {

    private static final int MAX_ADDRESSES_PER_MEMBER = 10;

    private final MemberRepository memberRepository;
    private final MemberAddressRepository addressRepository;
    private final Clock clock = Clock.systemUTC();

    public MemberAddressApplicationService(
            MemberRepository memberRepository,
            MemberAddressRepository addressRepository
    ) {
        this.memberRepository = memberRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberAddressView> getAddresses(UUID memberId) {
        requireMember(memberId);
        return addressRepository
                .findAllByMemberIdOrderByDefaultAddressDescCreatedAtAscIdAsc(memberId)
                .stream()
                .map(MemberAddress::toView)
                .toList();
    }

    @Transactional
    public MemberAddressView createAddress(
            UUID memberId,
            String addressName,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String addressLine1,
            String addressLine2,
            boolean requestedDefault
    ) {
        requireMember(memberId);
        if (addressRepository.countByMemberId(memberId) >= MAX_ADDRESSES_PER_MEMBER) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "배송지는 최대 10개까지 저장할 수 있습니다."
            );
        }
        String normalizedName = normalize(addressName);
        requireUniqueName(memberId, normalizedName, new UUID(0L, 0L));
        Instant now = clock.instant();
        boolean makeDefault = requestedDefault || addressRepository.countByMemberId(memberId) == 0;
        if (makeDefault) {
            addressRepository.clearDefaultAddress(memberId);
        }
        MemberAddress address = addressRepository.save(new MemberAddress(
                memberId,
                normalizedName,
                normalize(recipientName),
                normalize(phoneNumber),
                normalize(postalCode),
                normalize(addressLine1),
                normalizeNullable(addressLine2),
                makeDefault,
                now
        ));
        return address.toView();
    }

    @Transactional
    public MemberAddressView updateAddress(
            UUID memberId,
            UUID addressId,
            String addressName,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String addressLine1,
            String addressLine2,
            boolean requestedDefault
    ) {
        MemberAddress address = findAddress(memberId, addressId);
        String normalizedName = normalize(addressName);
        requireUniqueName(memberId, normalizedName, addressId);
        boolean makeDefault = requestedDefault || address.isDefaultAddress();
        if (requestedDefault) {
            addressRepository.clearDefaultAddress(memberId);
        }
        address.update(
                normalizedName,
                normalize(recipientName),
                normalize(phoneNumber),
                normalize(postalCode),
                normalize(addressLine1),
                normalizeNullable(addressLine2),
                makeDefault,
                clock.instant()
        );
        return address.toView();
    }

    @Transactional
    public void deleteAddress(UUID memberId, UUID addressId) {
        MemberAddress address = findAddress(memberId, addressId);
        boolean deletedDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (deletedDefault) {
            addressRepository
                    .findAllByMemberIdOrderByDefaultAddressDescCreatedAtAscIdAsc(memberId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> next.makeDefault(clock.instant()));
        }
    }

    private void requireMember(UUID memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }

    private MemberAddress findAddress(UUID memberId, UUID addressId) {
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DATA_NOT_FOUND,
                        "배송지를 찾을 수 없습니다."
                ));
    }

    private void requireUniqueName(UUID memberId, String addressName, UUID addressId) {
        if (addressRepository.existsByMemberIdAndAddressNameIgnoreCaseAndIdNot(
                memberId,
                addressName,
                addressId
        )) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "같은 이름의 배송지가 있습니다.");
        }
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
