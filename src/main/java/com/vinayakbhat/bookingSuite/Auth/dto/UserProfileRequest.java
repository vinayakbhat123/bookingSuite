package com.vinayakbhat.bookingSuite.Auth.dto;

import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserProfileRequest(
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        String name,

        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @Pattern(
                regexp = "^(\\+?[1-9]\\d{1,14})?$",
                message = "Phone number must follow E.164 international standard format (e.g., +1234567890)"
        )
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phoneNumber,

        @Past(message = "Birth date must be a date in the past")
        LocalDate birthDate,

        Gender gender,

        @Size(max = 255, message = "Bio cannot exceed 255 characters")
        String bio
) {
    public UserProfileRequest {
        if (name != null) name = name.strip();
        if (lastName != null) lastName = lastName.strip();
        if (phoneNumber != null) phoneNumber = phoneNumber.strip();
        if (bio != null) bio = bio.strip();
    }
}