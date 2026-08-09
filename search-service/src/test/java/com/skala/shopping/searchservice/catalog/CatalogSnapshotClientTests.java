package com.skala.shopping.searchservice.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatalogSnapshotClientTests {

    @Test
    void loadsCatalogPageWithoutDatabaseDependency() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://catalog.example/api/products?page=0&size=100"))
                .andRespond(withSuccess("""
                        {
                          "content": [{
                            "id": "11111111-1111-1111-1111-111111111111",
                            "name": "Catalog 상품",
                            "price": 10000.00,
                            "status": "ACTIVE"
                          }],
                          "page": 0,
                          "size": 100,
                          "totalElements": 1,
                          "totalPages": 1
                        }
                        """, MediaType.APPLICATION_JSON));
        CatalogSnapshotClient client = new CatalogSnapshotClient(
                builder.baseUrl("https://catalog.example").build());

        CatalogPage result = client.getProducts(0, 100);

        assertEquals(1, result.getTotalElements());
        assertEquals("Catalog 상품", result.getContent().getFirst().getName());
        server.verify();
    }
}
