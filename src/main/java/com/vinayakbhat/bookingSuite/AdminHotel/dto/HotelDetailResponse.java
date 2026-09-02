package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomDetailDto;

import java.math.BigDecimal;
import java.util.List;

public record HotelDetailResponse(
    Long hotelId,
    String hotelName,
    String cityName,
    String address,
    List<String> photos,
    List<String> amenities,
    BigDecimal calculatedTotalPrice,
    List<RoomDetailDto> rooms
) {}