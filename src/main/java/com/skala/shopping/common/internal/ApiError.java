package com.skala.shopping.common.internal;

import java.time.Instant;
import java.util.Map;

final class ApiError {

    private final String code;
    private final String message;
    private final int status;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;

    ApiError(
            String code,
            String message,
            int status,
            Instant timestamp,
            Map<String, String> fieldErrors
    ) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
        this.fieldErrors = fieldErrors;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
