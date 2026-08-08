package com.skala.shopping.returns.internal.web;

import com.skala.shopping.common.PageResponse;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.web.dto.request.UpdateReturnStatusRequest;
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

@RestController @RequestMapping("/api/admin/returns")
class AdminReturnController {
    private final ReturnApi returnApi;
    AdminReturnController(ReturnApi returnApi){this.returnApi=returnApi;}
    @GetMapping
    PageResponse<ReturnView> all(@RequestParam(defaultValue="0") @Min(0) int page,
                                 @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
        return returnApi.getAll(page,size);
    }
    @PutMapping("/{returnId}/status")
    ReturnView status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID returnId,
                      @RequestHeader("X-Idempotency-Key") UUID commandId,
                      @Valid @RequestBody UpdateReturnStatusRequest request){
        return returnApi.changeStatus(UUID.fromString(jwt.getSubject()),returnId,
                request.getStatus(),request.getAdminNote(),commandId);
    }
}
