package com.skala.shopping.member.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.member.MemberApi;
import com.skala.shopping.member.MemberResponse;
import com.skala.shopping.member.internal.domain.Member;
import com.skala.shopping.member.internal.domain.MemberStatus;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberApplicationService implements MemberApi {

    private static final String PASSWORD_RESET_FAILURE_MESSAGE =
            "입력한 회원 정보를 확인할 수 없습니다.";

    private final MemberRepository repository;
    private final Clock clock = Clock.systemUTC();

    public MemberApplicationService(MemberRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MemberResponse createMember(UUID memberId, String customerId, String name) {
        if (repository.existsByCustomerId(customerId)) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "이미 존재하는 고객입니다.");
        }
        return repository.save(new Member(memberId, customerId, name, clock.instant())).toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMember(UUID memberId) {
        return findById(memberId).toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "고객을 찾을 수 없습니다."))
                .toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getActiveMemberByIdentity(String customerId, String name) {
        return repository.findByCustomerIdAndNameAndStatus(
                        customerId,
                        name,
                        MemberStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        PASSWORD_RESET_FAILURE_MESSAGE
                ))
                .toResponse();
    }

    @Override
    @Transactional
    public void deactivateMember(UUID memberId) {
        findById(memberId).deactivate(clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getMembers(int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return PageResponse.from(repository.findAll(pageable).map(Member::toResponse));
    }

    @Transactional
    public MemberResponse updateName(UUID memberId, String name) {
        Member member = findById(memberId);
        member.updateName(name, clock.instant());
        return member.toResponse();
    }

    private Member findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "고객을 찾을 수 없습니다."));
    }
}
