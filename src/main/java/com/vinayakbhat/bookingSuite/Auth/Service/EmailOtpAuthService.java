package com.vinayakbhat.bookingSuite.Auth.Service;

import com.vinayakbhat.bookingSuite.Auth.dto.LoginResponse;

public interface EmailOtpAuthService {
    void sendOtp(String email);
    LoginResponse verifyOtpAndLogin(String email, String otpCode);
}
