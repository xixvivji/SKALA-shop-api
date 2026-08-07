package com.skala.shopping.common.internal.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@ExtendWith(OutputCaptureExtension.class)
class ApiLoggingAspectTests {

    private final ApiLoggingAspect aspect = new ApiLoggingAspect();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logsRouteTemplateAndStatusWithoutSensitiveRequestData(CapturedOutput output) throws Throwable {
        MockHttpServletRequest request = request("POST", "/api/customers/login");
        request.setContent("{\"customerPassword\":\"top-secret\"}"
                .getBytes(StandardCharsets.UTF_8));
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse())
        );
        ProceedingJoinPoint joinPoint = joinPoint(
                "AuthController.login(..)",
                ResponseEntity.ok().build()
        );

        aspect.logApiExecution(joinPoint);

        assertThat(output)
                .contains("api.completed")
                .contains("method=POST")
                .contains("path=/api/customers/login")
                .contains("handler=AuthController.login(..)")
                .contains("status=200")
                .doesNotContain("top-secret")
                .doesNotContain("customerPassword");
    }

    @Test
    void logsExceptionTypeAndRethrowsTheOriginalException(CapturedOutput output) throws Throwable {
        MockHttpServletRequest request = request("POST", "/api/orders/actual-order-id");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/orders/{orderId}"
        );
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse())
        );
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("OrderController.update(..)");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("database detail"));

        assertThatThrownBy(() -> aspect.logApiExecution(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database detail");

        assertThat(output)
                .contains("api.failed")
                .contains("path=/api/orders/{orderId}")
                .contains("exception=IllegalStateException")
                .doesNotContain("actual-order-id")
                .doesNotContain("database detail");
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, path);
        return request;
    }

    private ProceedingJoinPoint joinPoint(String signatureText, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn(signatureText);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }
}
