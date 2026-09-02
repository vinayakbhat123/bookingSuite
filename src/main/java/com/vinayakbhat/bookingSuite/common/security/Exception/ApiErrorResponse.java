package com.vinayakbhat.bookingSuite.common.security.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS) // Force Jackson to serialize null fields
public record ApiErrorResponse<T>(
        LocalDateTime time,
        T data,
        ApiError error
) {
    public static <T> ApiErrorResponse<T> success(T data) {
        return new ApiErrorResponse<>(LocalDateTime.now(), data, null);
    }

    public static <T> ApiErrorResponse<T> failure(ApiError error) {
        return new ApiErrorResponse<>(LocalDateTime.now(), null, error);
    }

    public static <T> ApiErrorResponse<T> failure(T data, ApiError error) {
        return new ApiErrorResponse<>(LocalDateTime.now(), data, error);
    }
}