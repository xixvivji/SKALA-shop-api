package com.skala.shopping.returns.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.web.dto.request.CreateReturnRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/returns")
class ReturnController {
    private final ReturnApi returnApi;
    ReturnController(ReturnApi returnApi){this.returnApi=returnApi;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ReturnView request(@AuthenticationPrincipal Jwt jwt,
                       @RequestHeader("X-Idempotency-Key") UUID commandId,
                       @Valid @RequestBody CreateReturnRequest request){
        return returnApi.request(memberId(jwt),request.getOrderId(),request.getOrderItemId(),
                request.getQuantity(),request.getReason(),request.getEvidenceImageUrl(),commandId);
    }
    @GetMapping("/me")
    PageResponse<ReturnView> mine(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(defaultValue="0") @Min(0) int page,
                                  @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
        return returnApi.getMine(memberId(jwt),page,size);
    }
    private UUID memberId(Jwt jwt){try{return UUID.fromString(jwt.getSubject());}
        catch(RuntimeException exception){throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);}}
}
