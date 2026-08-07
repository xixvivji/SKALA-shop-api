package com.skala.shopping.common.internal.aop;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 컨트롤러의 처리 시간과 결과만 기록하는 API 관측용 AOP입니다.
 *
 * <p>요청 본문, 쿼리 문자열, 헤더와 쿠키는 비밀번호나 JWT를 포함할 수 있으므로
 * 의도적으로 로그에 남기지 않습니다. 실제 경로 대신 매핑 패턴을 사용해 회원 ID 같은
 * 경로 변수도 로그에 노출하지 않습니다.</p>
 */
@Aspect
@Component
@ConditionalOnProperty(
        prefix = "shopping.logging.api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *) "
            + "&& execution(* com.skala.shopping..*(..))")
    Object logApiExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = requestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        long startedAt = System.nanoTime();
        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String path = routePattern(request);
        String handler = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            int status = result instanceof ResponseEntity<?> responseEntity
                    ? responseEntity.getStatusCode().value()
                    : attributes.getResponse() == null ? 200 : attributes.getResponse().getStatus();
            log.info(
                    "api.completed method={} path={} handler={} status={} durationMs={}",
                    method,
                    path,
                    handler,
                    status,
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (Throwable throwable) {
            // 예외 스택은 GlobalExceptionHandler가 한 번만 기록하고, 여기서는 요청 문맥만 남깁니다.
            log.info(
                    "api.failed method={} path={} handler={} exception={} durationMs={}",
                    method,
                    path,
                    handler,
                    throwable.getClass().getSimpleName(),
                    elapsedMillis(startedAt)
            );
            throw throwable;
        }
    }

    private ServletRequestAttributes requestAttributes() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes;
        }
        return null;
    }

    private String routePattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getRequestURI() : pattern.toString();
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
