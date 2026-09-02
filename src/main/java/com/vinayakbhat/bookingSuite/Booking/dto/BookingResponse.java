package com.vinayakbhat.bookingSuite.Booking.dto;

import com.vinayakbhat.bookingSuite.Booking.Enum.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record BookingResponse(
        Long id,
        BookingStatus status,
        Integer roomsCount,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Set<GuestResponse> guests,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal price,
        Long hotelId,
        String hotelName,
        String cityName,
        Long roomId,
        String roomType,
        String roomNumber,
        String stripeSessionId
) {
}
