package com.vinayakbhat.bookingSuite.Mapper;

import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;
import com.vinayakbhat.bookingSuite.Booking.Entity.Guest;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.GuestRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.hotelName")
    @Mapping(target = "cityName", source = "hotel.cityName")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomType", source = "room.roomType")
    @Mapping(target = "roomNumber", source = "room.roomNumber")
    @Mapping(target = "stripeSessionId", source = "stripeSessionId")
    BookingResponse toBookingResponse(Booking booking);

    Guest toGuestEntity(GuestRequest guestRequest);
}
