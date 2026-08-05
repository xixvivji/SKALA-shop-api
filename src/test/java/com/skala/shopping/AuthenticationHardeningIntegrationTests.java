package com.skala.shopping;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.auth.internal.AuthApplicationService;
import com.skala.shopping.common.BusinessException;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "shopping.security.bootstrap-admin.enabled=true",
        "shopping.security.bootstrap-admin.login-id=security-admin",
        "shopping.security.bootstrap-admin.password=security-admin-password",
        "shopping.security.rate-limit.login.max-requests-per-ip=2",
        "shopping.security.rate-limit.login.max-requests-per-account=10",
        "shopping.security.rate-limit.login.window=5m",
        "shopping.security.rate-limit.registration.max-requests-per-ip=2",
        "shopping.security.rate-limit.registration.max-requests-per-account=2",
        "shopping.security.rate-limit.registration.window=5m",
        "shopping.security.rate-limit.password-reset.max-requests-per-ip=10",
        "shopping.security.rate-limit.password-reset.max-requests-per-account=2",
        "shopping.security.rate-limit.password-reset.window=5m"
})
@AutoConfigureMockMvc
class AuthenticationHardeningIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuthApplicationService authApplicationService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Cookie csrfCookie;
    private String csrfToken;

    @BeforeEach
    void issueCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        csrfToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    @Test
    void returns429AndRetryAfterWhenLoginIpLimitIsExceeded() throws Exception {
        String clientAddress = "203.0.113.10";

        performLogin(unique("unknown-a"), "wrong-password", clientAddress)
                .andExpect(status().isUnauthorized());
        performLogin(unique("unknown-b"), "wrong-password", clientAddress)
                .andExpect(status().isUnauthorized());
        MvcResult limited = performLogin(unique("unknown-c"), "wrong-password", clientAddress)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andReturn();

        assertTrue(Long.parseLong(limited.getResponse().getHeader(HttpHeaders.RETRY_AFTER)) > 0);
    }

    @Test
    void limitsRegistrationByIpBeforeCreatingAnotherAccount() throws Exception {
        String clientAddress = "203.0.113.20";

        performRegistration(unique("registration-a"), clientAddress)
                .andExpect(status().isCreated());
        performRegistration(unique("registration-b"), clientAddress)
                .andExpect(status().isCreated());
        performRegistration(unique("registration-c"), clientAddress)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void limitsPasswordResetByAccountAcrossDifferentIps() throws Exception {
        String customerId = unique("reset-account");
        performRegistration(customerId, "203.0.113.30")
                .andExpect(status().isCreated());

        performPasswordReset(customerId, "잘못된 이름", "203.0.113.31")
                .andExpect(status().isBadRequest());
        performPasswordReset(customerId, "잘못된 이름", "203.0.113.32")
                .andExpect(status().isBadRequest());
        performPasswordReset(customerId, "잘못된 이름", "203.0.113.33")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void rotatesAdminPasswordAfterCurrentPasswordVerificationAndInvalidatesJwt() throws Exception {
        String oldPassword = "security-admin-password";
        String newPassword = "rotated-admin-password";
        Cookie adminCookie = successfulLogin(
                "security-admin",
                oldPassword,
                "203.0.113.40"
        );

        mockMvc.perform(put("/api/admin/password")
                        .with(clientAddress("203.0.113.40"))
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminPasswordBody("wrong-admin-password", newPassword)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/customers/list").cookie(copy(adminCookie)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/password")
                        .with(clientAddress("203.0.113.40"))
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminPasswordBody(oldPassword, newPassword)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));

        mockMvc.perform(get("/api/customers/list").cookie(copy(adminCookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));

        performLogin("security-admin", oldPassword, "203.0.113.41")
                .andExpect(status().isUnauthorized());
        successfulLogin("security-admin", newPassword, "203.0.113.42");

        UUID adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM auth.accounts WHERE login_id = ?",
                UUID.class,
                "security-admin"
        );
        String firstCandidate = "first-concurrent-password";
        String secondCandidate = "second-concurrent-password";
        List<String> outcomes = concurrently(
                () -> changeAdminPassword(adminId, newPassword, firstCandidate),
                () -> changeAdminPassword(adminId, newPassword, secondCandidate)
        );
        List<String> sortedOutcomes = new ArrayList<>(outcomes);
        Collections.sort(sortedOutcomes);
        assertEquals(List.of("NOT_AUTHENTICATED", "SUCCESS"), sortedOutcomes);

        String winningPassword = "SUCCESS".equals(outcomes.get(0))
                ? firstCandidate
                : secondCandidate;
        String losingPassword = "SUCCESS".equals(outcomes.get(0))
                ? secondCandidate
                : firstCandidate;
        performLogin("security-admin", winningPassword, "203.0.113.43")
                .andExpect(status().isOk());
        performLogin("security-admin", losingPassword, "203.0.113.44")
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions performRegistration(
            String customerId,
            String clientAddress
    ) throws Exception {
        return mockMvc.perform(post("/api/customers")
                .with(clientAddress(clientAddress))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "customerId": "%s",
                          "customerPassword": "customer-password",
                          "customerName": "보안 테스트 고객"
                        }
                        """.formatted(customerId)));
    }

    private org.springframework.test.web.servlet.ResultActions performPasswordReset(
            String customerId,
            String customerName,
            String clientAddress
    ) throws Exception {
        return mockMvc.perform(post("/api/customers/password/reset")
                .with(clientAddress(clientAddress))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "customerId": "%s",
                          "customerName": "%s",
                          "newPassword": "new-customer-password"
                        }
                        """.formatted(customerId, customerName)));
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(
            String customerId,
            String password,
            String clientAddress
    ) throws Exception {
        return mockMvc.perform(post("/api/customers/login")
                .with(clientAddress(clientAddress))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "customerId": "%s",
                          "customerPassword": "%s"
                        }
                        """.formatted(customerId, password)));
    }

    private Cookie successfulLogin(
            String customerId,
            String password,
            String clientAddress
    ) throws Exception {
        MvcResult result = performLogin(customerId, password, clientAddress)
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("bff-access");
        assertNotNull(cookie);
        return cookie;
    }

    private String adminPasswordBody(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);
    }

    private String changeAdminPassword(
            UUID adminId,
            String currentPassword,
            String newPassword
    ) {
        try {
            authApplicationService.changeAdminPassword(
                    adminId,
                    currentPassword,
                    newPassword
            );
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.errorCode().name();
        }
    }

    private <T> List<T> concurrently(
            Callable<T> firstAction,
            Callable<T> secondAction
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.function.Function<Callable<T>, Callable<T>> synchronize = action -> () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return action.call();
        };
        try {
            Future<T> first = executor.submit(synchronize.apply(firstAction));
            Future<T> second = executor.submit(synchronize.apply(secondAction));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Cookie copy(Cookie cookie) {
        return new Cookie(cookie.getName(), cookie.getValue());
    }

    private RequestPostProcessor clientAddress(String clientAddress) {
        return request -> {
            request.setRemoteAddr(clientAddress);
            return request;
        };
    }

    private RequestPostProcessor csrf() {
        return request -> {
            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.removeIf(cookie -> "XSRF-TOKEN".equals(cookie.getName()));
            cookies.add(copy(csrfCookie));
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", csrfToken);
            return request;
        };
    }
}
