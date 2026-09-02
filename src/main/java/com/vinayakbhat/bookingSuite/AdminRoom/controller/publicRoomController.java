package com.vinayakbhat.bookingSuite.AdminRoom.controller;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/rooms")
public class publicRoomController {
    private final RoomService roomService;

    public publicRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

      //
    // public api
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long hotelId, @PathVariable Long roomId) {
        RoomResponse response = roomService.getRoom(hotelId,roomId);
        return ResponseEntity.ok(response);
    }
    // public Api
    @GetMapping("/allrooms")
    public ResponseEntity<List<RoomResponse>> getAllHotelRooms(@PathVariable Long hotelId) {
        List<RoomResponse> response = roomService.getAllHotelRooms(hotelId);
        return ResponseEntity.ok(response);
    }
}
