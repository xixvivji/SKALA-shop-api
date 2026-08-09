package com.skala.shopping.searchservice.catalog;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 초기 색인과 수동 복구 시 공개 Catalog API만 사용하며 Backend RDS에는 접근하지 않습니다. */
@Component
public class CatalogSnapshotClient {

    private final RestClient restClient;

    @Autowired
    public CatalogSnapshotClient(
            RestClient.Builder builder,
            @Value("${shopping.catalog.base-url:http://localhost:8080}") String baseUrl,
            @Value("${shopping.catalog.connect-timeout:2s}") Duration connectTimeout,
            @Value("${shopping.catalog.read-timeout:10s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    CatalogSnapshotClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public CatalogPage getProducts(int page, int size) {
        CatalogPage response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/products")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(CatalogPage.class);
        if (response == null) {
            throw new IllegalStateException("Catalog API가 빈 응답을 반환했습니다.");
        }
        return response;
    }
}
