package com.skala.shopping.returns.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.web.dto.request.UpdateReturnStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/returns")
@Tag(name = "관리자 반품", description = "전체 반품 신청 조회와 처리 상태 관리")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class AdminReturnController {

    private final ReturnApi returnApi;

    AdminReturnController(ReturnApi returnApi) {
        this.returnApi = returnApi;
    }

    @GetMapping
    @Operation(
            summary = "전체 반품 신청 조회",
            description = "관리자가 모든 고객의 반품 신청을 최신순으로 페이지 조회합니다."
    )
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<ReturnView> all(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return returnApi.getAll(page, size);
    }

    @PutMapping("/{returnId}/status")
    @Operation(
            summary = "반품 처리 상태 변경",
            description = "반품의 회수·검수·승인·거절 상태와 관리자 메모를 멱등하게 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경된 반품 정보",
                    content = @Content(schema = @Schema(implementation = ReturnView.class))),
            @ApiResponse(responseCode = "400", description = "반품 ID, 상태 또는 멱등성 키 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "반품 신청을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전이 또는 멱등성 충돌",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ReturnView status(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID returnId,
            @Parameter(description = "상태 변경 재시도에 동일하게 사용하는 UUID", required = true)
            @RequestHeader("X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody UpdateReturnStatusRequest request
    ) {
        return returnApi.changeStatus(
                UUID.fromString(jwt.getSubject()),
                returnId,
                request.getStatus(),
                request.getAdminNote(),
                commandId
        );
    }
}
