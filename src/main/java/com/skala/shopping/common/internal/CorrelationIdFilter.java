package com.skala.shopping.common.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 전체를 하나의 식별자로 연결해 애플리케이션·Nginx·분산 서비스 로그를 추적합니다.
 * 외부에서 전달된 값은 로그 주입을 막기 위해 제한된 문자와 길이만 허용합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

    static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final String headerName;

    CorrelationIdFilter(
            @Value("${shopping.observability.correlation-header:X-Correlation-ID}") String headerName
    ) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = normalize(request.getHeader(headerName));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(headerName, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String normalize(String candidate) {
        if (candidate != null && SAFE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
