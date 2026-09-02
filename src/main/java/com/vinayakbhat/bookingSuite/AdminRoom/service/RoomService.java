package com.vinayakbhat.bookingSuite.AdminRoom.service;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomRequest;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;

import java.util.List;

public interface RoomService {
    RoomResponse createRoom(Long hotelId,RoomRequest request);
    RoomResponse updateRoom(Long hotelId, Long roomId,RoomRequest request);
    RoomResponse getRoom(Long hotelId,Long roomId);
    List<RoomResponse> getAllHotelRooms(Long hotelId);
    String deleteRoom(Long roomId);

}
