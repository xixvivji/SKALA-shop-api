package com.skala.shopping.notificationservice.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<ApiErrorResponse> notFound(EmptyResultDataAccessException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                "DATA_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND.value()
        ));
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiErrorResponse> invalidParameter(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_PARAMETER",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        ));
    }
}
