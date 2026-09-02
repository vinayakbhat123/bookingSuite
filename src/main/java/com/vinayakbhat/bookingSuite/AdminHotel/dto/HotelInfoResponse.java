package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;

import java.util.List;

public record HotelInfoResponse(
        HotelResponse hotel,
        List<RoomResponse> rooms
) {
}
