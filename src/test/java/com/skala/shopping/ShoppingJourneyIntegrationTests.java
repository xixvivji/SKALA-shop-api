package com.skala.shopping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
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

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SKALA Shop API"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in").value("cookie"))
                .andExpect(jsonPath("$['paths']['/api/products']").exists());
    }

    @Test
    void completesSignUpLoginOrderAndPartialCancellation() throws Exception {
        mockMvc.perform(post("/api/customers")
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

        var createdProduct = mockMvc.perform(post("/api/products")
                        .cookie(authCookie)
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
                        .cookie(authCookie)
                        .header("X-Idempotency-Key", orderCommandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPoints").value(970_000));

        mockMvc.perform(post("/api/customers/order")
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

        mockMvc.perform(post("/api/customers/cancel")
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
}
