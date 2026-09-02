package com.vinayakbhat.bookingSuite.Booking.dto;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;

public record GuestResponse(
        Long id,
        String name,
        Gender gender,
        Integer age
) {
}
