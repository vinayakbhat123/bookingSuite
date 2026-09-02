package com.vinayakbhat.bookingSuite.Booking.dto;

import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;

public record GuestRequest(
        Long id,
        User user,
        String name,
        Gender gender,
        Integer age
) {
}
