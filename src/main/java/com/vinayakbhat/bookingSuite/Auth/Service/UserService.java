package com.vinayakbhat.bookingSuite.Auth.Service;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileRequest;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;

import java.util.List;

public interface UserService {
    User getUserById(Long id);

    String updateProfile(UserProfileRequest request);

    UserProfileResponse getUserProfile(Long userId);

    List<BookingResponse> getMyBookings();
}



