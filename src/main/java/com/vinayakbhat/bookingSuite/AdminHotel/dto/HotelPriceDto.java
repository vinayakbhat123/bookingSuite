package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record HotelPriceDto(
        Long hotelId,
        String hotelName,
        String cityName,
        List<String> photos,
        List<String> amenities,
        BigDecimal price
) implements Serializable {
}