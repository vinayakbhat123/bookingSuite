package com.vinayakbhat.bookingSuite.common.security.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        HttpStatus status,
        String message,
        List<String> errors
) {
    // 1. Convenience constructor for a single error message
    public ApiError(HttpStatus status, String message) {
        this(status, message, List.of(message));
    }

    // 2. Convenience constructor defaulting to BAD_REQUEST
    public ApiError(String message, List<String> errors) {
        this(HttpStatus.BAD_REQUEST, message, errors);
    }
}