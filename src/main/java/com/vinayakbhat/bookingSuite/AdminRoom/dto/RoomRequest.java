package com.vinayakbhat.bookingSuite.AdminRoom.dto;

import com.vinayakbhat.bookingSuite.AdminRoom.Enum.RoomStatus;
import com.vinayakbhat.bookingSuite.AdminRoom.Enum.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record RoomRequest(
        Long id,

        @NotNull(message = "Room type is required")
        RoomType roomType,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Base price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Base price must be a valid monetary amount (up to 8 digits and 2 decimal places)")
        BigDecimal basePrice,

        @NotNull(message = "Total count is required")
        @Min(value = 1, message = "Total count must be at least 1")
        Integer totalCount,

        @NotNull(message = "Floor number is required")
        @Min(value = 0, message = "Floor number cannot be negative")
        @Max(value = 50,message = "Floor number cannot be greater than 50")
        Integer floor,

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1 guest")
        @Max(value = 20, message = "capacity cannot exceed 20 guests")
        Integer capacity,

        RoomStatus status,

        List<@NotBlank(message = "Photo URL cannot be empty") String> photos,

        List<@NotBlank(message = "Amenity cannot be empty") String> amenities
) {

}