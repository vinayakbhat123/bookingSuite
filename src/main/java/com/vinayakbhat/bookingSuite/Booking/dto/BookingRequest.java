package com.vinayakbhat.bookingSuite.Booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingRequest(

        @NotNull(message = "Hotel ID is required")
        @Min(value = 1, message = "Hotel ID must be greater than 0")
        Long hotelId,

        @NotNull(message = "Room ID is required")
        @Min(value = 1, message = "Room ID must be greater than 0")
        Long roomId,

        @NotNull(message = "Check-in date is required")
        @FutureOrPresent(message = "Check-in date must be today or in the future")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        @FutureOrPresent(message = "Check-out date must be today or in the future")
        LocalDate checkOutDate,

        @NotNull(message = "Rooms count is required")
        @Min(value = 1, message = "Rooms count must be at least 1")
        Integer roomsCount

) {
}