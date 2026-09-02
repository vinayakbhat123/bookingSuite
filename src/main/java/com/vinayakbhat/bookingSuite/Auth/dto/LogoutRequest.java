package com.vinayakbhat.bookingSuite.Auth.dto;

public record LogoutRequest(
        String refreshToken
) {}