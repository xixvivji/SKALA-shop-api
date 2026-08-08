package com.skala.shopping.common.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filter = new CorrelationIdFilter("X-Correlation-ID");

    @Test
    void preservesSafeClientCorrelationIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "checkout-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observed = new AtomicReference<>();
        FilterChain chain = (ignoredRequest, ignoredResponse) ->
                observed.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals("checkout-123", observed.get());
        assertEquals("checkout-123", response.getHeader("X-Correlation-ID"));
        assertFalse(MDC.getCopyOfContextMap() != null
                && MDC.getCopyOfContextMap().containsKey(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "unsafe\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        String generated = response.getHeader("X-Correlation-ID");
        assertFalse(generated.contains("\n"));
        assertFalse(generated.equals("unsafe\nvalue"));
    }
}
