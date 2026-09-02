package com.vinayakbhat.bookingSuite.Auth.controller;

import com.vinayakbhat.bookingSuite.Auth.Service.AuthService;
import com.vinayakbhat.bookingSuite.Auth.Service.EmailOtpAuthService;
import com.vinayakbhat.bookingSuite.Auth.dto.*;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import com.vinayakbhat.bookingSuite.common.security.Exception.UnAuthorisedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailOtpAuthService otpAuthService;

    public AuthController(AuthService authService, EmailOtpAuthService otpAuthService) {
        this.authService = authService;
        this.otpAuthService = otpAuthService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request){
        return new ResponseEntity<>(ApiResponse.success(authService.signup(request),"Signup Successfully"),HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        String[] loginResponse = authService.login(request);
        Cookie cookie = new Cookie("refreshToken",loginResponse[1]);
        cookie.setHttpOnly(true);
        httpServletResponse.addCookie(cookie); 
        return  new ResponseEntity<>(ApiResponse.success(new LoginResponse(loginResponse[0]),"Login Successfully"), HttpStatus.CREATED);
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, @RequestBody(required = false) LogoutRequest logoutRequest) {
        authService.logout(request, logoutRequest);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully. Tokens revoked."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshToken = Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new UnAuthorisedException("Refresh token not found"));

        String accessToken = authService.refreshToken(refreshToken);
        return new ResponseEntity<>(ApiResponse.success(new LoginResponse(accessToken)), HttpStatus.CREATED);
    }
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestParam String email) {
        otpAuthService.sendOtp(email);
        return ResponseEntity.ok(ApiResponse.success( "OTP sent successfully to email"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(otpAuthService.verifyOtpAndLogin(request.email(), request.otpCode()), "Login successful via OTP"));
    }
}
