package com.vinayakbhat.bookingSuite.Auth.controller;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Service.UserService;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileRequest;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(@AuthenticationPrincipal User currentUser) {
        UserProfileResponse response = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<String>> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(request)));
    }

    @PutMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<String>> updateProfilePut(@Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(request)));
    }
    @GetMapping("/mybookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        return ResponseEntity.ok(userService.getMyBookings());
    }

}
