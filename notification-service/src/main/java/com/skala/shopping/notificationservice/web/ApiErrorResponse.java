package com.skala.shopping.notificationservice.web;

import java.time.Instant;

public final class ApiErrorResponse {

    private final String code;
    private final String message;
    private final int status;
    private final Instant timestamp;

    public ApiErrorResponse(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
}
