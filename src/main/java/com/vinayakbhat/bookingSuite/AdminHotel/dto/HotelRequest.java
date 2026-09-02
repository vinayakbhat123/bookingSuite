package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record HotelRequest(

        Long id,                                    // Used for update, null for create

        @NotBlank(message = "hotelName is required")
        @Size(min = 2, max = 200, message = "hotelName must be between 2 and 200 characters")
        String hotelName,

        @Size(max = 100, message = "cityName must not exceed 100 characters")
        String cityName,

        @Size(max = 50, message = "Maximum 50 photos allowed")
        String[] photos,

        @Size(max = 100, message = "Maximum 100 amenities allowed")
        String[] amenities,

        @NotNull(message = "contactInfo is required")
        @Valid                                      // Cascades validation into embedded object
        HotelContactRequest contactInfo,

        Boolean active
) {
    // Compact constructor - handles sanitization and default fallback
    public HotelRequest {
        if (hotelName != null) {
            hotelName = hotelName.trim();
        }
        if (cityName != null) {
            cityName = cityName.trim();
        }

    }
}