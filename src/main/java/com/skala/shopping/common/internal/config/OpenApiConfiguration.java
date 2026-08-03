package com.skala.shopping.common.internal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    static final String COOKIE_SECURITY_SCHEME = "cookieAuth";

    @Bean
    OpenAPI shopOpenApi(
            @Value("${shopping.security.cookie.name:bff-access}") String cookieName
    ) {
        SecurityScheme cookieSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(cookieName)
                .description("로그인 API가 발급하는 HttpOnly JWT 쿠키");

        return new OpenAPI()
                .info(new Info()
                        .title("SKALA Shop API")
                        .version("v1")
                        .description("모듈러 모놀리스 기반 쇼핑몰 백엔드 API"))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_SECURITY_SCHEME, cookieSecurityScheme));
    }
}
