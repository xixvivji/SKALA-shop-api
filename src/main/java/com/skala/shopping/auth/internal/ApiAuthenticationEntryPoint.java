package com.skala.shopping.auth.internal;

import com.skala.shopping.common.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorResponseWriter responseWriter;

    ApiAuthenticationEntryPoint(ApiErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        responseWriter.write(response, ErrorCode.NOT_AUTHENTICATED);
    }
}
