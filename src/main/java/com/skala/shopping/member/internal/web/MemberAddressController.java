package com.skala.shopping.member.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.member.internal.MemberAddressApplicationService;
import com.skala.shopping.member.internal.web.dto.request.SaveMemberAddressRequest;
import com.skala.shopping.member.internal.web.dto.response.MemberAddressResponse;
import io.swagger.v3.oas.annotations.Operation;
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
class MemberAddressController {

    private final MemberAddressApplicationService service;

    MemberAddressController(MemberAddressApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "내 배송지 목록")
    List<MemberAddressResponse> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        return service.getAddresses(memberId(jwt)).stream()
                .map(MemberAddressResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "배송지 저장")
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
    @Operation(summary = "배송지 수정")
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
    @Operation(summary = "배송지 삭제")
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
