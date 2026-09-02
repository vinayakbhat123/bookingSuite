package com.vinayakbhat.bookingSuite.AdminHotel.dto;

public record HotelStatusResponse(
        Long id,                                    // used for update, null for create
        String hotelName,
        String cityName// optional – defaults to ACTIVE in entity
) {
}
