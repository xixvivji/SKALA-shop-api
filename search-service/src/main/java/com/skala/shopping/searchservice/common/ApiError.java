package com.skala.shopping.searchservice.common;

import java.time.Instant;

public final class ApiError {

    private final String code;
    private final String message;
    private final Instant timestamp;

    public ApiError(String code, String message, Instant timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
