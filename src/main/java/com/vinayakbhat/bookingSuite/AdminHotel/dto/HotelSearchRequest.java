package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HotelSearchRequest(

        @NotBlank(message = "City is required and cannot be blank")
        String city,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        LocalDate endDate,

        @NotNull(message = "Rooms count is required")
        @Min(value = 1, message = "Rooms count must be at least 1")
        Integer roomsCount,

        @Min(value = 0, message = "Page number must be non-negative")
        Integer pageNumber,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100") // Prevent OOM memory exhaustion
        Integer pageSize
) {
    // Canonical Compact Constructor for Safe Defaults
    public HotelSearchRequest {
        pageNumber = (pageNumber == null || pageNumber < 0) ? 0 : pageNumber;
        pageSize = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
    }

    // Cross-field Bean Validation check
    @AssertTrue(message = "End date must be at least one day after start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true; // Handled by @NotNull
        }
        return endDate.isAfter(startDate);
    }
}