package com.skala.shopping.notificationservice.web;

import com.skala.shopping.notificationservice.service.NotificationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "로그인한 회원의 주문·재입고 알림을 조회하고 읽음 처리합니다.")
@SecurityRequirement(name = "cookieAuth")
public class NotificationController {

    private final NotificationApplicationService service;

    public NotificationController(NotificationApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "내 알림 목록", description = "최신 알림부터 페이지 단위로 조회합니다.")
    public NotificationPageResponse getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.getNotifications(memberId(jwt), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 수", description = "현재 회원의 읽지 않은 알림 개수를 반환합니다.")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountResponse(service.getUnreadCount(memberId(jwt)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "본인 소유 알림을 읽음 상태로 변경합니다.")
    public NotificationResponse markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notificationId
    ) {
        return service.markRead(memberId(jwt), notificationId);
    }

    private UUID memberId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
