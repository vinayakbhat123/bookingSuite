package com.vinayakbhat.bookingSuite.common.security.Exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXCLUDED_ROUTES = List.of(
            "/api/v1/v3/api-docs/**",
            "/api/v1/swagger-ui/**",
            "/api/v1/swagger-ui.html",
            "/api/v1/swagger-resources/**",
            "/api/v1/actuator/**"
    );

    public GlobalResponseHandler(@Lazy ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body,
                                            MethodParameter returnType,
                                            MediaType selectedContentType,
                                            Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                            ServerHttpRequest request,
                                            ServerHttpResponse response) {

        String path = request.getURI().getPath();

        // 1. Skip wrapping for excluded routes using proper Ant-style path matching
        boolean isExcluded = EXCLUDED_ROUTES.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isExcluded || body instanceof ApiResponse<?> || body instanceof ApiErrorResponse<?>) {
            return body;
        }

        ApiResponse<Object> apiResponse = ApiResponse.success(body);

        // 2. Handle String return types explicitly to avoid ClassCastException
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Error serializing ApiResponse to JSON", e);
            }
        }

        return apiResponse;
    }
}