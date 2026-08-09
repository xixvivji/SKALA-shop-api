package com.skala.shopping.search.internal;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Search Service의 내부 HTTP 계약만 담당하며 Elasticsearch 구현을 Backend에서 분리합니다. */
@Component
@ConditionalOnProperty(name = "shopping.search.enabled", havingValue = "true")
class SearchServiceClient {

    private final RestClient restClient;

    @Autowired
    SearchServiceClient(
            RestClient.Builder builder,
            @Value("${shopping.search.base-url:http://localhost:8081}") String baseUrl,
            @Value("${shopping.search.connect-timeout:2s}") Duration connectTimeout,
            @Value("${shopping.search.read-timeout:3s}") Duration readTimeout,
            @Value("${shopping.observability.correlation-header:X-Correlation-ID}") String correlationHeader
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set(correlationHeader, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    SearchServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    SearchPage search(String query, int page, int size) {
        SearchPage response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/search/products")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(SearchPage.class);
        if (response == null) {
            throw new IllegalStateException("검색 서비스가 빈 응답을 반환했습니다.");
        }
        return response;
    }

    long reindex() {
        ReindexResult response = restClient.post()
                .uri("/internal/search/reindex")
                .retrieve()
                .body(ReindexResult.class);
        if (response == null) {
            throw new IllegalStateException("검색 서비스가 빈 응답을 반환했습니다.");
        }
        return response.getIndexed();
    }

    String openApi() {
        String response = restClient.get()
                .uri("/v3/api-docs")
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("검색 서비스 API 명세가 비어 있습니다.");
        }
        return response;
    }

    public static final class SearchPage {

        private List<SearchProduct> content = new ArrayList<>();
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public SearchPage() {
        }

        public List<SearchProduct> getContent() {
            return content;
        }

        public void setContent(List<SearchProduct> content) {
            this.content = content == null ? new ArrayList<>() : content;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }

    public static final class SearchProduct {

        private UUID id;
        private String name;
        private BigDecimal price;
        private UUID categoryId;
        private String description;
        private String imageUrl;
        private String status;

        public SearchProduct() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public UUID getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(UUID categoryId) {
            this.categoryId = categoryId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static final class ReindexResult {

        private long indexed;

        public ReindexResult() {
        }

        public long getIndexed() {
            return indexed;
        }

        public void setIndexed(long indexed) {
            this.indexed = indexed;
        }
    }
}
