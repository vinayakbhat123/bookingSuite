package com.vinayakbhat.bookingSuite.common.security.Exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleBusinessException(ResourceNotFoundException ex) {
        log.warn("Resource Not Found: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.failure(null,apiError));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleUserNameNotFoundException(UsernameNotFoundException ex) {
        log.warn("Username Not Found: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.failure(null,apiError));
    }

    @ExceptionHandler(UnAuthorisedException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleUnAuthorisedException(UnAuthorisedException ex) {
        log.warn("You Are UnAuthorised: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.failure(null,apiError));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleJwtException(JwtException ex) {
        log.warn("Jwt Token Expired: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.failure(null,apiError));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("AccessDenied: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.failure(null,apiError));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Spring Security AccessDenied: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.failure(null, apiError));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.failure(null, apiError));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("DTO validation failed");

        List<String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields",
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 4. Handle Unique Constraint Violations / Database Exceptions (409 Conflict)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Database constraint error: {}", ex.getMostSpecificCause().getMessage());

        String message = "Database constraint violation. Check for unique key conflicts (e.g. duplicate hotel name).";
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT,
                message,
                List.of(ex.getMostSpecificCause().getMessage())
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 5. Handle Type Mismatch / Invalid Enum Request Parameters (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detailMessage = String.format("Parameter '%s' has an invalid value '%s'", ex.getName(), ex.getValue());

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            detailMessage = String.format("Invalid value '%s' for parameter '%s'. Permitted values are: %s",
                    ex.getValue(), ex.getName(), Arrays.toString(ex.getRequiredType().getEnumConstants()));
        }

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, detailMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 6. Handle Malformed JSON or Bad Body Parsing (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Failed to parse HTTP request body: {}", ex.getMessage());

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON payload or invalid field types in request body"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 7. Handle Business State Exceptions (400 Bad Request)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state conflict: {}", ex.getMessage());
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Invalid operation in current state"
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 8. Handle Illegal Arguments & Validation Checks (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided"
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.failure(apiError));
    }

    // 9. Fallback Exception Handler for Unhandled Runtime Errors (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Unhandled internal server exception", ex);
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred. Please contact system support.",
                List.of(ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "Internal server error")
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.failure(apiError));
    }
}