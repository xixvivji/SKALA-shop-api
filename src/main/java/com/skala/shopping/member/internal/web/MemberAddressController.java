package com.skala.shopping.member.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.member.internal.MemberAddressApplicationService;
import com.skala.shopping.member.internal.web.dto.request.SaveMemberAddressRequest;
import com.skala.shopping.member.internal.web.dto.response.MemberAddressResponse;
import com.skala.shopping.common.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/me/addresses")
@Tag(name = "회원 배송지", description = "내 저장 배송지 관리")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class MemberAddressController {

    private final MemberAddressApplicationService service;

    MemberAddressController(MemberAddressApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "내 배송지 목록", description = "로그인한 고객이 저장한 배송지 목록을 조회합니다.")
    List<MemberAddressResponse> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        return service.getAddresses(memberId(jwt)).stream()
                .map(MemberAddressResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "배송지 저장", description = "주문에 사용할 새 배송지를 내 주소록에 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "저장된 배송지"),
            @ApiResponse(responseCode = "400", description = "배송지 입력값 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "배송지 이름 중복",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<MemberAddressResponse> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveMemberAddressRequest request
    ) {
        MemberAddressResponse response = MemberAddressResponse.from(service.createAddress(
                memberId(jwt),
                request.getAddressName(),
                request.getRecipientName(),
                request.getPhoneNumber(),
                request.getPostalCode(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.isDefaultAddress()
        ));
        return ResponseEntity
                .created(URI.create("/api/customers/me/addresses/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "배송지 수정", description = "내 주소록에 저장된 배송지 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "배송지 ID 또는 입력값 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "배송지 이름 중복",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    MemberAddressResponse updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody SaveMemberAddressRequest request
    ) {
        return MemberAddressResponse.from(service.updateAddress(
                memberId(jwt),
                addressId,
                request.getAddressName(),
                request.getRecipientName(),
                request.getPhoneNumber(),
                request.getPostalCode(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.isDefaultAddress()
        ));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "배송지 삭제", description = "내 주소록에서 선택한 배송지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "배송지 삭제 완료"),
            @ApiResponse(responseCode = "400", description = "배송지 ID 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        service.deleteAddress(memberId(jwt), addressId);
        return ResponseEntity.noContent().build();
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
