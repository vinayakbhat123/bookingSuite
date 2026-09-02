package com.vinayakbhat.bookingSuite.Auth.dto;

import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String lastName,
        String phoneNumber,
        LocalDate birthDate,
        Gender gender,
        String bio,
        Set<Role> roles
) {
}
