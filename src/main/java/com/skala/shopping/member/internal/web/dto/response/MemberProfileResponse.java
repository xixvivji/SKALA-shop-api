package com.skala.shopping.member.internal.web.dto.response;

import com.skala.shopping.common.PageResponse;
import com.skala.shopping.member.MemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "MemberProfileResponse", description = "회원 프로필 응답")
public final class MemberProfileResponse {

    @Schema(description = "회원 식별자")
    private final UUID memberId;

    @Schema(description = "로그인에 사용하는 고객 ID", example = "skala01")
    private final String customerId;

    @Schema(description = "고객 이름", example = "김스칼라")
    private final String name;

    @Schema(description = "회원 상태", example = "ACTIVE")
    private final String status;

    public MemberProfileResponse(UUID memberId, String customerId, String name, String status) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.name = name;
        this.status = status;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public static MemberProfileResponse from(MemberResponse member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getCustomerId(),
                member.getName(),
                member.getStatus()
        );
    }

    public static PageResponse<MemberProfileResponse> pageFrom(PageResponse<MemberResponse> members) {
        return new PageResponse<>(
                members.getContent().stream().map(MemberProfileResponse::from).toList(),
                members.getPage(),
                members.getSize(),
                members.getTotalElements(),
                members.getTotalPages()
        );
    }
}
