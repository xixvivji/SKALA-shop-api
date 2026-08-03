package com.skala.shopping;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "shopping.security.bootstrap-admin.password=integration-admin-password"
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
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['400']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['403']").exists())
                .andExpect(jsonPath("$['paths']['/api/orders']['post']['responses']['409']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/logout']['post']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/csrf']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/customers/me']['get']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequest").exists())
                .andExpect(jsonPath("$.components.schemas.CancelOrderRequest").exists())
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
                                  "productPrice": 15000
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode productJson = objectMapper.readTree(createdProduct.getResponse().getContentAsString());
        UUID productId = UUID.fromString(productJson.get("id").asText());

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
                .andExpect(jsonPath("$.length()").value(1));

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
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get("/api/products?page=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(copy(customer.authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(UUID.randomUUID(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
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
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/customers/me").cookie(copy(customer.authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPoint").value(1_000_000));
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
        MvcResult result = mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .cookie(copy(adminCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(productName, productPrice)))
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
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<MvcResult> synchronizedRequest = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return request.call();
        };
        try {
            Future<MvcResult> first = executor.submit(synchronizedRequest);
            Future<MvcResult> second = executor.submit(synchronizedRequest);
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

    private String registrationBody(String customerId) {
        return """
                {
                  "customerId": "%s",
                  "customerPassword": "pw123456",
                  "customerName": "테스트 고객"
                }
                """.formatted(customerId);
    }

    private String loginBody(String customerId, String password) {
        return """
                {
                  "customerId": "%s",
                  "customerPassword": "%s"
                }
                """.formatted(customerId, password);
    }

    private String productBody(String productName, String productPrice) {
        return """
                {
                  "productName": "%s",
                  "productPrice": %s
                }
                """.formatted(productName, productPrice);
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
