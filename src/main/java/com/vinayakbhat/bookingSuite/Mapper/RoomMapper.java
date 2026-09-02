package com.vinayakbhat.bookingSuite.Mapper;

import com.vinayakbhat.bookingSuite.AdminRoom.Enum.RoomStatus;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomRequest;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "status", expression = "java(getDefaultStatus())")
    Room fromRequestToRoom(RoomRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomResponse toRoomResponse(Room room);

    // Updates existing entity in-place without overriding id, hotel, or roomNumber with null
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    void updateRoomFromDto(RoomRequest request, @MappingTarget Room room);
    default RoomStatus getDefaultStatus() {
        return RoomStatus.UNAVAILABLE;
    }
}
