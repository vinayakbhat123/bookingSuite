package com.vinayakbhat.bookingSuite.Auth.Service;

import com.vinayakbhat.bookingSuite.Auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthService {
    AuthResponse signup(SignupRequest signupRequest);
    String[] login(LoginRequest loginRequest);
    String refreshToken(String refreshToken);
    String logout(HttpServletRequest request, LogoutRequest logoutRequest);
}
