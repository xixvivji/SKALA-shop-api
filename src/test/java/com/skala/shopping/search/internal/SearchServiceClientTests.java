package com.skala.shopping.search.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SearchServiceClientTests {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void sendsSearchRequestAndCorrelationIdToPrivateService() {
        RestClient.Builder builder = clientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SearchServiceClient client = new SearchServiceClient(builder.build());
        MDC.put("correlationId", "corr-search-1");
        server.expect(once(), requestTo("http://search.internal:8081/internal/search/products?query=%ED%82%A4%EB%B3%B4%EB%93%9C&page=0&size=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Correlation-ID", "corr-search-1"))
                .andRespond(withSuccess("""
                        {"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
                        """, MediaType.APPLICATION_JSON));

        SearchServiceClient.SearchPage result = client.search("키보드", 0, 20);

        assertEquals(0, result.getTotalElements());
        server.verify();
    }

    @Test
    void delegatesReindexWithoutExposingElasticsearch() {
        RestClient.Builder builder = clientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SearchServiceClient client = new SearchServiceClient(builder.build());
        server.expect(once(), requestTo("http://search.internal:8081/internal/search/reindex"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"indexed\":18}", MediaType.APPLICATION_JSON));

        assertEquals(18L, client.reindex());
        server.verify();
    }

    private RestClient.Builder clientBuilder() {
        return RestClient.builder()
                .baseUrl("http://search.internal:8081")
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-ID", correlationId);
                    }
                    return execution.execute(request, body);
                });
    }
}
