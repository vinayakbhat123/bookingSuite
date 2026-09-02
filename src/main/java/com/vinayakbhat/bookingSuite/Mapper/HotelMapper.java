package com.vinayakbhat.bookingSuite.Mapper;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelStatusResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    Hotel toHotel(HotelRequest hotel);
    HotelRequest toHotelRequest(Hotel hotel);
    HotelStatusResponse toHotelStatusResponse(Hotel hotel);
    HotelResponse toHotelResponse(Hotel hotel);
    RoomResponse toRoomResponse(Room room);

}

