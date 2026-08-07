package com.skala.shopping;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
        "shopping.security.bootstrap-admin.login-id=integration-admin",
        "shopping.security.bootstrap-admin.password=integration-admin-password",
        "shopping.security.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
class ShoppingJourneyIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Cookie csrfCookie;
    private String csrfToken;

    @BeforeEach
    void issueRealCsrfToken() throws Exception {
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
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("requestInterceptor")))
                .andExpect(content().string(containsString("X-XSRF-TOKEN")));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SKALA Shop API"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in").value("cookie"))
                .andExpect(jsonPath("$['paths']['/api/products']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/products']['post']['responses']['403']").exists())
                .andExpect(jsonPath("$['paths']['/api/products/{productId}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/products/stocks']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/products/{productId}/stock']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/products/{productId}/stock']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/products/{productId}/stock/adjustments']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['400']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['403']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['409']").exists())
                .andExpect(jsonPath("$['paths']['/api/cart']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/cart/items']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/me/addresses']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/categories']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/admin/orders']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/wallet/me/transactions']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/logout']['post']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/password/reset']['post']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/csrf']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/me']['get']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequest").exists())
                .andExpect(jsonPath("$.components.schemas.CancelOrderRequest").exists())
                .andExpect(jsonPath("$.components.schemas.AdjustStockRequest").exists())
                .andExpect(jsonPath("$.components.schemas.InitializeStockRequest").exists())
                .andExpect(jsonPath("$.components.schemas.StockResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ResetPasswordRequest").exists())
                .andExpect(jsonPath("$.components.schemas.CsrfTokenResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    @Test
    void completesSignUpLoginOrderAndPartialCancellation() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "skala01",
                                  "customerPassword": "pw1234",
                                  "customerName": "테스트 고객"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerPoint").value(1_000_000));

        var login = mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "skala01",
                                  "customerPassword": "pw1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Cookie authCookie = login.getResponse().getCookie("bff-access");

        var adminLogin = mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "integration-admin",
                                  "customerPassword": "integration-admin-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        Cookie adminCookie = adminLogin.getResponse().getCookie("bff-access");

        var createdProduct = mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "무선마우스",
                                  "productPrice": 15000,
                                  "initialQuantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode productJson = objectMapper.readTree(createdProduct.getResponse().getContentAsString());
        UUID productId = UUID.fromString(productJson.get("id").asText());

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(productId.toString()));

        String orderBody = """
                {
                  "productId": "%s",
                  "quantity": 2
                }
                """.formatted(productId);
        String orderCommandId = "11111111-1111-4111-8111-111111111111";

        mockMvc.perform(post("/api/customers/order")
                        .with(csrf())
                        .cookie(authCookie)
                        .header("X-Idempotency-Key", orderCommandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPoints").value(970_000));

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(8));

        mockMvc.perform(post("/api/customers/order")
                        .with(csrf())
                        .cookie(authCookie)
                        .header("X-Idempotency-Key", orderCommandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPoints").value(970_000));

        mockMvc.perform(get("/api/orders/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/customers/skala01").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(970_000))
                .andExpect(jsonPath("$.products[0].quantity").value(2));

        mockMvc.perform(get("/api/customers/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("skala01"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerPoint").value(970_000));

        mockMvc.perform(post("/api/customers/cancel")
                        .with(csrf())
                        .cookie(authCookie)
                        .header(
                                "X-Idempotency-Key",
                                "22222222-2222-4222-8222-222222222222"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(15_000))
                .andExpect(jsonPath("$.remainingPoints").value(985_000));

        mockMvc.perform(get("/api/customers/skala01").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(985_000))
                .andExpect(jsonPath("$.products[0].quantity").value(1));

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(9));
    }

    @Test
    void issuesCsrfTokenAndRequiresItForStateChanges() throws Exception {
        var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        String token = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token")
                .asText();
        String customerId = unique("csrf");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/customers")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isCreated());
    }

    @Test
    void resetsPasswordWithCustomerIdAndNameAndStoresOnlyBcryptHashes() throws Exception {
        String customerId = unique("password-reset");
        String oldPassword = "pw123456";
        String newPassword = "newPw123456";

        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isCreated());

        String oldHash = passwordHash(customerId);
        assertTrue(oldHash.startsWith("$2"));
        assertNotEquals(oldPassword, oldHash);
        Cookie staleAuthCookie = login(customerId, oldPassword);

        mockMvc.perform(put("/api/customers/me")
                        .with(csrf())
                        .cookie(copy(staleAuthCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "현재 고객 이름"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("현재 고객 이름"));

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "테스트 고객", newPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력한 회원 정보를 확인할 수 없습니다."));

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "현재 고객 이름", newPassword)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/customers/me").cookie(copy(staleAuthCookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, oldPassword)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, newPassword)))
                .andExpect(status().isOk());

        String newHash = passwordHash(customerId);
        assertTrue(newHash.startsWith("$2"));
        assertNotEquals(newPassword, newHash);
        assertNotEquals(oldHash, newHash);
    }

    @Test
    void usesOneGenericErrorForUnknownCustomerAndWrongNameDuringPasswordReset() throws Exception {
        String customerId = unique("password-identity");
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "다른 이름", "newPw123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("입력한 회원 정보를 확인할 수 없습니다."));

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(unique("unknown"), "테스트 고객", "newPw123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("입력한 회원 정보를 확인할 수 없습니다."));

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, "pw123456")))
                .andExpect(status().isOk());
    }

    @Test
    void validatesRequiredCustomerNameAndPasswordResetInputs() throws Exception {
        String customerId = unique("required-name");
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "customerPassword": "pw123456",
                                  "customerName": " "
                                }
                                """.formatted(customerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName").exists());

        mockMvc.perform(post("/api/customers/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "테스트 고객", "newPw123456")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "테스트 고객", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(customerId, "테스트 고객", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void handlesMultibytePasswordsBeyondTheBcryptByteLimitWithoutServerErrors() throws Exception {
        String customerId = unique("bcrypt-bytes");
        String oversizedMultibytePassword = "가".repeat(25);
        assertTrue(oversizedMultibytePassword.length() <= 72);
        assertTrue(oversizedMultibytePassword.getBytes(StandardCharsets.UTF_8).length > 72);

        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                customerId,
                                oversizedMultibytePassword,
                                "테스트 고객"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.customerPassword")
                        .value("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."));

        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, oversizedMultibytePassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"))
                .andExpect(jsonPath("$.message")
                        .value("고객 ID 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/api/customers/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody(
                                customerId,
                                "테스트 고객",
                                oversizedMultibytePassword
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.newPassword")
                        .value("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."));

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, "pw123456")))
                .andExpect(status().isOk());
    }

    @Test
    void returnsConsistentAuthenticationAuthorizationAndInputErrors() throws Exception {
        CustomerSession customer = registerAndLogin("security");
        Cookie adminCookie = loginAdmin();

        mockMvc.perform(get("/api/orders/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));

        mockMvc.perform(get("/api/customers/list").cookie(copy(customer.authCookie)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/customers/list").cookie(copy(adminCookie)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/me").cookie(copy(adminCookie)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(unique("blocked-product"), "15000")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/products/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.productId")
                        .value("요청 값의 형식이 올바르지 않습니다."));

        mockMvc.perform(get("/api/products?page=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.page")
                        .value("요청 값의 형식이 올바르지 않습니다."));

        mockMvc.perform(get("/api/orders/me?page=-1").cookie(copy(customer.authCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.page").exists());

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(copy(customer.authCookie))
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.orderShapeValid")
                        .value("productId/quantity 또는 items를 입력해야 합니다."));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(UUID.randomUUID(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors['X-Idempotency-Key']")
                        .value("필수 요청 헤더입니다."));

        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors._request")
                        .value("요청 본문의 JSON 형식이 올바르지 않습니다."));
    }

    @Test
    void clearsCookiesOnLogoutAndDeactivationAndRejectsOldJwt() throws Exception {
        var invalidCookie = new Cookie("bff-access", "invalid-token");
        mockMvc.perform(post("/api/customers/logout")
                        .with(csrf())
                        .cookie(invalidCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        CustomerSession customer = registerAndLogin("deactivate");
        mockMvc.perform(delete("/api/customers/me")
                        .with(csrf())
                        .cookie(copy(customer.authCookie)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(get("/api/orders/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));

        mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customer.customerId, customer.password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    void scopesIdempotencyToCustomerAndRejectsDifferentPayload() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(adminCookie, unique("idempotent-product"), "15000");
        CustomerSession firstCustomer = registerAndLogin("idempotent-a");
        CustomerSession secondCustomer = registerAndLogin("idempotent-b");
        UUID commandId = UUID.randomUUID();

        var firstOrder = performOrder(firstCustomer.authCookie, productId, 2, commandId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingPoints").value(970_000))
                .andReturn();
        String firstOrderId = objectMapper.readTree(firstOrder.getResponse().getContentAsString())
                .get("id")
                .asText();

        performOrder(firstCustomer.authCookie, productId, 2, commandId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstOrderId))
                .andExpect(jsonPath("$.remainingPoints").value(970_000));

        performOrder(firstCustomer.authCookie, productId, 1, commandId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        performOrder(secondCustomer.authCookie, productId, 1, commandId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingPoints").value(985_000));

        UUID cancellationId = UUID.randomUUID();
        var cancellation = performCancellation(
                firstCustomer.authCookie,
                productId,
                1,
                cancellationId
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPoints").value(985_000))
                .andReturn();
        String cancellationResultId = objectMapper
                .readTree(cancellation.getResponse().getContentAsString())
                .get("id")
                .asText();

        performOrder(firstCustomer.authCookie, productId, 1, UUID.randomUUID())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingPoints").value(970_000));

        performCancellation(firstCustomer.authCookie, productId, 1, cancellationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cancellationResultId))
                .andExpect(jsonPath("$.remainingPoints").value(985_000));
    }

    @Test
    void replaysOriginalOrderCreationResponseAfterPartialCancellation() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(adminCookie, unique("order-replay"), "15000");
        CustomerSession customer = registerAndLogin("order-replay");
        UUID orderCommandId = UUID.randomUUID();

        MvcResult original = performOrder(
                customer.authCookie,
                productId,
                2,
                orderCommandId
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.canceledAmount").value(0))
                .andExpect(jsonPath("$.remainingPoints").value(970_000))
                .andExpect(jsonPath("$.items[0].canceledQuantity").value(0))
                .andReturn();
        JsonNode originalResponse = objectMapper.readTree(
                original.getResponse().getContentAsString()
        );

        performCancellation(customer.authCookie, productId, 1, UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPoints").value(985_000));

        MvcResult replay = performOrder(
                customer.authCookie,
                productId,
                2,
                orderCommandId
        )
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(
                originalResponse,
                objectMapper.readTree(replay.getResponse().getContentAsString())
        );
        mockMvc.perform(get("/api/orders/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PARTIALLY_CANCELED"))
                .andExpect(jsonPath("$.content[0].canceledAmount").value(15_000))
                .andExpect(jsonPath("$.content[0].items[0].canceledQuantity").value(1));
    }

    @Test
    void pagesOrdersWithStableNewestFirstSorting() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(adminCookie, unique("order-page"), "100");
        CustomerSession customer = registerAndLogin("order-page");
        UUID firstCommand = UUID.randomUUID();
        UUID secondCommand = UUID.randomUUID();
        UUID thirdCommand = UUID.randomUUID();

        performOrder(customer.authCookie, productId, 1, firstCommand)
                .andExpect(status().isCreated());
        performOrder(customer.authCookie, productId, 1, secondCommand)
                .andExpect(status().isCreated());
        performOrder(customer.authCookie, productId, 1, thirdCommand)
                .andExpect(status().isCreated());

        jdbcTemplate.update(
                """
                        UPDATE orders.orders
                        SET ordered_at = TIMESTAMPTZ '2026-01-01 00:00:00+00'
                        WHERE request_id IN (?, ?, ?)
                        """,
                firstCommand,
                secondCommand,
                thirdCommand
        );
        List<String> expectedIds = jdbcTemplate.queryForList(
                """
                        SELECT id::text
                        FROM orders.orders
                        WHERE request_id IN (?, ?, ?)
                        ORDER BY ordered_at DESC, id DESC
                        """,
                String.class,
                firstCommand,
                secondCommand,
                thirdCommand
        );

        mockMvc.perform(get("/api/orders/me?page=0&size=2")
                        .cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(expectedIds.get(1)));

        mockMvc.perform(get("/api/orders/me?page=1&size=2")
                        .cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(2)));
    }

    @Test
    void preservesProductNameRulesAndPricePrecision() throws Exception {
        Cookie adminCookie = loginAdmin();
        String productName = unique("Mouse");
        UUID productId = createProduct(adminCookie, "  " + productName + "  ", "15000");

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(productName.toLowerCase(), "16000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_DUPLICATED"));

        mockMvc.perform(delete("/api/products/{productId}", productId)
                        .with(csrf())
                        .cookie(copy(adminCookie)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderable").value(false))
                .andExpect(jsonPath("$.stockStatus").value("INACTIVE"));

        performStockAdjustment(
                adminCookie,
                productId,
                1,
                "삭제 상품 입고 시도",
                UUID.randomUUID()
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_SALEABLE"));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(productName.toLowerCase(), "16000")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(unique("fraction"), "1.234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void rollsBackOrderWhenPointsAreInsufficient() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(adminCookie, unique("expensive"), "600000");
        CustomerSession customer = registerAndLogin("rollback");

        performOrder(customer.authCookie, productId, 2, UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/api/orders/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/customers/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(1_000_000));

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void managesStockWithIdempotentAdminAdjustments() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("stock-adjustment"),
                "15000",
                5
        );

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.maxOrderQuantity").value(5))
                .andExpect(jsonPath("$.orderable").value(true))
                .andExpect(jsonPath("$.stockStatus").value("LOW_STOCK"));

        UUID operationId = UUID.randomUUID();
        performStockAdjustment(adminCookie, productId, 10, "신규 입고", operationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"));

        performStockAdjustment(adminCookie, productId, 10, "신규 입고", operationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        performStockAdjustment(adminCookie, productId, 9, "신규 입고", operationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        performStockAdjustment(
                adminCookie,
                productId,
                -15,
                "재고 실사 반영",
                UUID.randomUUID()
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(0))
                .andExpect(jsonPath("$.orderable").value(false))
                .andExpect(jsonPath("$.stockStatus").value("OUT_OF_STOCK"));
    }

    @Test
    void keepsLegacyProductCreateRequestCompatibleWithDefaultStock() throws Exception {
        Cookie adminCookie = loginAdmin();
        MvcResult created = mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "%s",
                                  "productPrice": 15000
                                }
                                """.formatted(unique("legacy-product"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID productId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void replaysConcurrentLegacyStockInitializationOnce() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = insertLegacyProduct("legacy-stock");
        UUID operationId = UUID.randomUUID();

        List<MvcResult> results = concurrently(() -> performStockInitializationResult(
                adminCookie,
                productId,
                7,
                operationId
        ));

        assertEquals(List.of(200, 200), statuses(results));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory.stocks WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory.stock_movements WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(7));
    }

    @Test
    void rejectsUnknownOrDeletedLegacyStockInitialization() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID unknownProductId = UUID.randomUUID();

        performStockInitialization(
                adminCookie,
                unknownProductId,
                10,
                UUID.randomUUID()
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DATA_NOT_FOUND"));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory.stocks WHERE product_id = ?",
                        Integer.class,
                        unknownProductId
                )
        );

        UUID deletedProductId = insertLegacyProduct("deleted-legacy-stock");
        mockMvc.perform(delete("/api/products/{productId}", deletedProductId)
                        .with(csrf())
                        .cookie(copy(adminCookie)))
                .andExpect(status().isNoContent());

        performStockInitialization(
                adminCookie,
                deletedProductId,
                10,
                UUID.randomUUID()
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DATA_NOT_FOUND"));

        mockMvc.perform(get("/api/products/{productId}/stock", deletedProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(0))
                .andExpect(jsonPath("$.stockStatus").value("INACTIVE"));
    }

    @Test
    void replaysOriginalInitializationSnapshotAfterProductDeletion() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = insertLegacyProduct("deleted-initialization-replay");
        UUID operationId = UUID.randomUUID();

        MvcResult original = performStockInitialization(
                adminCookie,
                productId,
                7,
                operationId
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(7))
                .andExpect(jsonPath("$.maxOrderQuantity").value(7))
                .andExpect(jsonPath("$.orderable").value(true))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                .andReturn();

        mockMvc.perform(delete("/api/products/{productId}", productId)
                        .with(csrf())
                        .cookie(copy(adminCookie)))
                .andExpect(status().isNoContent());

        MvcResult replay = performStockInitialization(
                adminCookie,
                productId,
                7,
                operationId
        )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(
                objectMapper.readTree(original.getResponse().getContentAsString()),
                objectMapper.readTree(replay.getResponse().getContentAsString())
        );

        performStockInitialization(adminCookie, productId, 8, operationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(7))
                .andExpect(jsonPath("$.maxOrderQuantity").value(0))
                .andExpect(jsonPath("$.orderable").value(false))
                .andExpect(jsonPath("$.stockStatus").value("INACTIVE"));
    }

    @Test
    void replaysOriginalAdjustmentSnapshotAfterProductDeletion() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("deleted-adjustment-replay"),
                "15000",
                5
        );
        UUID operationId = UUID.randomUUID();

        MvcResult original = performStockAdjustment(
                adminCookie,
                productId,
                10,
                "삭제 전 입고",
                operationId
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.orderable").value(true))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                .andReturn();

        mockMvc.perform(delete("/api/products/{productId}", productId)
                        .with(csrf())
                        .cookie(copy(adminCookie)))
                .andExpect(status().isNoContent());

        MvcResult replay = performStockAdjustment(
                adminCookie,
                productId,
                10,
                "삭제 전 입고",
                operationId
        )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(
                objectMapper.readTree(original.getResponse().getContentAsString()),
                objectMapper.readTree(replay.getResponse().getContentAsString())
        );

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.maxOrderQuantity").value(0))
                .andExpect(jsonPath("$.orderable").value(false))
                .andExpect(jsonPath("$.stockStatus").value("INACTIVE"));
    }

    @Test
    void keepsStockInactiveWhenLegacyInitializationRacesWithDeletion() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = insertLegacyProduct("legacy-init-delete-race");

        List<MvcResult> results = concurrently(
                () -> performStockInitializationResult(
                        adminCookie,
                        productId,
                        10,
                        UUID.randomUUID()
                ),
                () -> mockMvc.perform(delete("/api/products/{productId}", productId)
                                .with(csrf())
                                .cookie(copy(adminCookie)))
                        .andReturn()
        );

        assertTrue(results.stream().anyMatch(result -> result.getResponse().getStatus() == 204));
        assertTrue(results.stream()
                .filter(result -> result.getResponse().getStatus() != 204)
                .allMatch(result -> List.of(200, 404, 409)
                        .contains(result.getResponse().getStatus())));
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderable").value(false))
                .andExpect(jsonPath("$.stockStatus").value("INACTIVE"));
    }

    @Test
    void rollsBackPointsWhenStockIsInsufficient() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("insufficient-stock"),
                "15000",
                1
        );
        CustomerSession customer = registerAndLogin("insufficient-stock");

        performOrder(customer.authCookie, productId, 2, UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(get("/api/customers/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(1_000_000));

        mockMvc.perform(get("/api/orders/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(1));
    }

    @Test
    void allowsOnlyOneCustomerToOrderTheLastUnit() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("last-unit"),
                "15000",
                1
        );
        CustomerSession firstCustomer = registerAndLogin("last-unit-a");
        CustomerSession secondCustomer = registerAndLogin("last-unit-b");

        List<MvcResult> results = concurrently(
                () -> performOrderResult(
                        firstCustomer.authCookie,
                        productId,
                        1,
                        UUID.randomUUID()
                ),
                () -> performOrderResult(
                        secondCustomer.authCookie,
                        productId,
                        1,
                        UUID.randomUUID()
                )
        );

        assertEquals(List.of(201, 409), statuses(results));
        MvcResult failed = results.stream()
                .filter(result -> result.getResponse().getStatus() == 409)
                .findFirst()
                .orElseThrow();
        assertEquals(
                "INSUFFICIENT_STOCK",
                objectMapper.readTree(failed.getResponse().getContentAsString()).get("code").asText()
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM orders.order_items WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM inventory.stock_movements
                        WHERE product_id = ? AND movement_type = 'RESERVE'
                        """,
                        Integer.class,
                        productId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM wallet.point_transactions AS point_transaction
                        JOIN orders.orders AS shop_order
                          ON shop_order.id = point_transaction.reference_id
                        JOIN orders.order_items AS order_item
                          ON order_item.order_id = shop_order.id
                        WHERE order_item.product_id = ?
                          AND point_transaction.transaction_type = 'DEBIT'
                        """,
                        Integer.class,
                        productId
                )
        );
        List<Integer> balances = new ArrayList<>(List.of(
                currentPoint(firstCustomer.authCookie),
                currentPoint(secondCustomer.authCookie)
        ));
        Collections.sort(balances);
        assertEquals(List.of(985_000, 1_000_000), balances);

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(0));
    }

    @Test
    void rollsBackCancellationWhenStockReleaseFails() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("release-rollback"),
                "15000",
                1
        );
        CustomerSession customer = registerAndLogin("release-rollback");
        performOrder(customer.authCookie, productId, 1, UUID.randomUUID())
                .andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE inventory.stocks SET available_quantity = ? WHERE product_id = ?",
                Integer.MAX_VALUE,
                productId
        );

        performCancellation(customer.authCookie, productId, 1, UUID.randomUUID())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        assertEquals(985_000, currentPoint(customer.authCookie));
        mockMvc.perform(get("/api/orders/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.content[0].items[0].canceledQuantity").value(0));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM orders.order_cancellations WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM inventory.stock_movements
                        WHERE product_id = ? AND movement_type = 'RELEASE'
                        """,
                        Integer.class,
                        productId
                )
        );
    }

    @Test
    void allowsOnlyOneConcurrentCancellationForOnePurchasedUnit() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(
                adminCookie,
                unique("concurrent-cancellation"),
                "15000",
                1
        );
        CustomerSession customer = registerAndLogin("concurrent-cancellation");
        performOrder(customer.authCookie, productId, 1, UUID.randomUUID())
                .andExpect(status().isCreated());

        List<MvcResult> results = concurrently(
                () -> performCancellationResult(
                        customer.authCookie,
                        productId,
                        1,
                        UUID.randomUUID()
                ),
                () -> performCancellationResult(
                        customer.authCookie,
                        productId,
                        1,
                        UUID.randomUUID()
                )
        );

        assertEquals(List.of(200, 409), statuses(results));
        assertEquals(1_000_000, currentPoint(customer.authCookie));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM orders.order_cancellations WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM inventory.stock_movements
                        WHERE product_id = ? AND movement_type = 'RELEASE'
                        """,
                        Integer.class,
                        productId
                )
        );

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(1));
    }

    @Test
    void handlesConcurrentOrderAndCancellationRetriesOnce() throws Exception {
        Cookie adminCookie = loginAdmin();
        UUID productId = createProduct(adminCookie, unique("concurrent"), "15000");
        CustomerSession customer = registerAndLogin("concurrent");
        UUID orderCommandId = UUID.randomUUID();

        List<MvcResult> orderResults = concurrently(() -> performOrderResult(
                customer.authCookie,
                productId,
                2,
                orderCommandId
        ));
        assertEquals(List.of(201, 201), statuses(orderResults));
        assertEquals(
                responseId(orderResults.get(0)),
                responseId(orderResults.get(1))
        );

        UUID cancellationCommandId = UUID.randomUUID();
        List<MvcResult> cancellationResults = concurrently(() -> performCancellationResult(
                customer.authCookie,
                productId,
                1,
                cancellationCommandId
        ));
        assertEquals(List.of(200, 200), statuses(cancellationResults));
        assertEquals(
                responseId(cancellationResults.get(0)),
                responseId(cancellationResults.get(1))
        );

        mockMvc.perform(get("/api/customers/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(985_000))
                .andExpect(jsonPath("$.products[0].quantity").value(1));

        mockMvc.perform(get("/api/products/{productId}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(99));

        assertEquals(
                3,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory.stock_movements WHERE product_id = ?",
                        Integer.class,
                        productId
                )
        );
    }

    @Test
    void allowsOnlyConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ));

        mockMvc.perform(options("/api/orders")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private CustomerSession registerAndLogin(String prefix) throws Exception {
        String customerId = unique(prefix);
        String password = "pw123456";
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(customerId)))
                .andExpect(status().isCreated());
        return new CustomerSession(customerId, password, login(customerId, password));
    }

    @Test
    void supportsCartSavedAddressMultiItemOrderFulfillmentAndLedgers() throws Exception {
        Cookie admin = loginAdmin();
        UUID first = createProduct(admin, unique("multi-first"), "10000", 10);
        UUID second = createProduct(admin, unique("multi-second"), "20000", 10);
        CustomerSession customer = registerAndLogin("multi-order");

        mockMvc.perform(post("/api/customers/me/addresses").with(csrf()).cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"addressName":"집","recipientName":"테스트 고객","phoneNumber":"010-1234-5678",
                                 "postalCode":"12345","addressLine1":"서울시 테스트로 1","defaultAddress":true}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.defaultAddress").value(true));

        mockMvc.perform(post("/api/cart/items").with(csrf()).cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"quantity\":2}".formatted(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(2));
        mockMvc.perform(post("/api/cart/items").with(csrf()).cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"quantity\":1}".formatted(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(40000));

        MvcResult placed = mockMvc.perform(post("/api/orders").with(csrf()).cookie(copy(customer.authCookie))
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"items":[{"productId":"%s","quantity":2},{"productId":"%s","quantity":1}],
                                 "shippingAddress":{"recipientName":"테스트 고객","phoneNumber":"010-1234-5678",
                                 "postalCode":"12345","addressLine1":"서울시 테스트로 1"}}
                                """.formatted(first, second)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.remainingPoints").value(960000))
                .andExpect(jsonPath("$.fulfillmentStatus").value("PAID"))
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("12345")).andReturn();
        UUID orderId = UUID.fromString(objectMapper.readTree(placed.getResponse().getContentAsString()).get("id").asText());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders.order_shipping_addresses WHERE order_id = ?", Integer.class, orderId));

        mockMvc.perform(put("/api/admin/orders/{orderId}/fulfillment", orderId).with(csrf()).cookie(copy(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fulfillmentStatus").value("PREPARING"));
        mockMvc.perform(put("/api/admin/orders/{orderId}/fulfillment", orderId).with(csrf()).cookie(copy(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fulfillmentStatus").value("SHIPPED"));
        mockMvc.perform(get("/api/admin/orders/{orderId}/history", orderId).cookie(copy(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].toStatus").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/cancellations").with(csrf()).cookie(copy(customer.authCookie))
                        .header("X-Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"quantity\":1}".formatted(first)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/wallet/me/transactions").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(get("/api/products/{productId}/stock/movements", first).cookie(copy(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
    }

    private Cookie loginAdmin() throws Exception {
        return login("integration-admin", "integration-admin-password");
    }

    private Cookie login(String customerId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customers/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(customerId, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("bff-access");
        assertNotNull(cookie);
        return cookie;
    }

    private UUID createProduct(
            Cookie adminCookie,
            String productName,
            String productPrice
    ) throws Exception {
        return createProduct(adminCookie, productName, productPrice, 100);
    }

    private UUID createProduct(
            Cookie adminCookie,
            String productName,
            String productPrice,
            int initialQuantity
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(productName, productPrice, initialQuantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions performOrder(
            Cookie authCookie,
            UUID productId,
            int quantity,
            UUID commandId
    ) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .with(csrf())
                .cookie(copy(authCookie))
                .header("X-Idempotency-Key", commandId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(productId, quantity)));
    }

    private org.springframework.test.web.servlet.ResultActions performCancellation(
            Cookie authCookie,
            UUID productId,
            int quantity,
            UUID commandId
    ) throws Exception {
        return mockMvc.perform(post("/api/orders/cancellations")
                .with(csrf())
                .cookie(copy(authCookie))
                .header("X-Idempotency-Key", commandId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(productId, quantity)));
    }

    private org.springframework.test.web.servlet.ResultActions performStockAdjustment(
            Cookie adminCookie,
            UUID productId,
            int quantityDelta,
            String reason,
            UUID operationId
    ) throws Exception {
        return mockMvc.perform(post("/api/products/{productId}/stock/adjustments", productId)
                .with(csrf())
                .cookie(copy(adminCookie))
                .header("X-Idempotency-Key", operationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "quantityDelta": %d,
                          "reason": "%s"
                        }
                        """.formatted(quantityDelta, reason)));
    }

    private org.springframework.test.web.servlet.ResultActions performStockInitialization(
            Cookie adminCookie,
            UUID productId,
            int availableQuantity,
            UUID operationId
    ) throws Exception {
        return mockMvc.perform(post("/api/products/{productId}/stock", productId)
                .with(csrf())
                .cookie(copy(adminCookie))
                .header("X-Idempotency-Key", operationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availableQuantity": %d
                        }
                        """.formatted(availableQuantity)));
    }

    private MvcResult performStockInitializationResult(
            Cookie adminCookie,
            UUID productId,
            int availableQuantity,
            UUID operationId
    ) throws Exception {
        return performStockInitialization(
                adminCookie,
                productId,
                availableQuantity,
                operationId
        ).andReturn();
    }

    private MvcResult performOrderResult(
            Cookie authCookie,
            UUID productId,
            int quantity,
            UUID commandId
    ) throws Exception {
        return performOrder(authCookie, productId, quantity, commandId).andReturn();
    }

    private MvcResult performCancellationResult(
            Cookie authCookie,
            UUID productId,
            int quantity,
            UUID commandId
    ) throws Exception {
        return performCancellation(authCookie, productId, quantity, commandId).andReturn();
    }

    private List<MvcResult> concurrently(Callable<MvcResult> request) throws Exception {
        return concurrently(request, request);
    }

    private List<MvcResult> concurrently(
            Callable<MvcResult> firstRequest,
            Callable<MvcResult> secondRequest
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.function.Function<Callable<MvcResult>, Callable<MvcResult>> synchronize = request -> () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return request.call();
        };
        try {
            Future<MvcResult> first = executor.submit(synchronize.apply(firstRequest));
            Future<MvcResult> second = executor.submit(synchronize.apply(secondRequest));
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

    private List<Integer> statuses(List<MvcResult> results) {
        List<Integer> statuses = new ArrayList<>(results.stream()
                .map(result -> result.getResponse().getStatus())
                .toList());
        Collections.sort(statuses);
        return statuses;
    }

    private String responseId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private int currentPoint(Cookie authCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/customers/me").cookie(copy(authCookie)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("customerPoint")
                .asInt();
    }

    private UUID insertLegacyProduct(String prefix) {
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO catalog.products (
                    id, name, price, status, version, created_at, updated_at
                ) VALUES (?, ?, 15000, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                productId,
                unique(prefix)
        );
        return productId;
    }

    private String registrationBody(String customerId) {
        return registrationBody(customerId, "pw123456", "테스트 고객");
    }

    private String registrationBody(String customerId, String password, String customerName) {
        return """
                {
                  "customerId": "%s",
                  "customerPassword": "%s",
                  "customerName": "%s"
                }
                """.formatted(customerId, password, customerName);
    }

    private String loginBody(String customerId, String password) {
        return """
                {
                  "customerId": "%s",
                  "customerPassword": "%s"
                }
                """.formatted(customerId, password);
    }

    private String resetPasswordBody(String customerId, String customerName, String newPassword) {
        return """
                {
                  "customerId": "%s",
                  "customerName": "%s",
                  "newPassword": "%s"
                }
                """.formatted(customerId, customerName, newPassword);
    }

    private String passwordHash(String customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT password_hash FROM auth.accounts WHERE login_id = ?",
                String.class,
                customerId
        );
    }

    private String productBody(String productName, String productPrice) {
        return productBody(productName, productPrice, 100);
    }

    private String productBody(
            String productName,
            String productPrice,
            int initialQuantity
    ) {
        return """
                {
                  "productName": "%s",
                  "productPrice": %s,
                  "initialQuantity": %d
                }
                """.formatted(productName, productPrice, initialQuantity);
    }

    private String orderBody(UUID productId, int quantity) {
        return """
                {
                  "productId": "%s",
                  "quantity": %d
                }
                """.formatted(productId, quantity);
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Cookie copy(Cookie cookie) {
        return new Cookie(cookie.getName(), cookie.getValue());
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

    private static final class CustomerSession {

        private final String customerId;
        private final String password;
        private final Cookie authCookie;

        private CustomerSession(String customerId, String password, Cookie authCookie) {
            this.customerId = customerId;
            this.password = password;
            this.authCookie = authCookie;
        }
    }
}
