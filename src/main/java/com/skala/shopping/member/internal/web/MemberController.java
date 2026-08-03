package com.skala.shopping.member.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.member.MemberResponse;
import com.skala.shopping.member.internal.MemberApplicationService;
import com.skala.shopping.member.internal.web.dto.request.UpdateMemberRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "회원", description = "고객 목록과 내 정보 관리")
@SecurityRequirement(name = "cookieAuth")
class MemberController {

    private final MemberApplicationService service;

    MemberController(MemberApplicationService service) {
        this.service = service;
    }

    @GetMapping("/list")
    @Operation(summary = "고객 목록 조회")
    PageResponse<MemberResponse> getMembers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return service.getMembers(page, size);
    }

    @PutMapping("/me")
    @Operation(summary = "내 이름 변경")
    MemberResponse updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMemberRequest request
    ) {
        return service.updateName(memberId(jwt), request.getName());
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }

}
