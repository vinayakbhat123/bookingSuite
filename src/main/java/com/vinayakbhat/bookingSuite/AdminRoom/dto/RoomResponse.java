package com.vinayakbhat.bookingSuite.AdminRoom.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomResponse(
        Long id,
        Long hotelId,
        String roomNumber,
        String roomType,
        BigDecimal basePrice,
        Integer totalCount,
        Integer floor,
        Integer capacity,
        List<String> photos,
        List< String> amenities
) {
}
