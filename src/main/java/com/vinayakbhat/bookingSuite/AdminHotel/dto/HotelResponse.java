package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.HotelContactInfo;

public record HotelResponse(
        Long id,                                    // used for update, null for create
        String hotelName,
        String cityName,
        String[] photos,
        String[] amenities,
        HotelContactInfo contactInfo,
        Boolean active
) {
}
