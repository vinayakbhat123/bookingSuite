package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HotelContactRequest(
        @NotBlank String address,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phoneNumber,
        @Email String email,
        String location
) {
    public HotelContactRequest{
        if (address != null) address = address.trim();
        if (email != null) email = email.toLowerCase().trim();
    }
}
