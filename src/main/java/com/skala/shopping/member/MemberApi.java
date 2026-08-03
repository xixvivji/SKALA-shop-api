package com.skala.shopping.member;

import java.util.UUID;

public interface MemberApi {

    MemberResponse createMember(UUID memberId, String customerId, String name);

    MemberResponse getMember(UUID memberId);

    MemberResponse getMemberByCustomerId(String customerId);

    void deactivateMember(UUID memberId);
}
