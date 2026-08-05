package com.skala.shopping.member;

import java.util.UUID;

public interface MemberApi {

    MemberResponse createMember(UUID memberId, String customerId, String name);

    MemberResponse getMember(UUID memberId);

    MemberResponse getMemberByCustomerId(String customerId);

    MemberResponse getActiveMemberByIdentity(String customerId, String name);

    void deactivateMember(UUID memberId);
}
