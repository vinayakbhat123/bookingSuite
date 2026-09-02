package com.vinayakbhat.bookingSuite.AdminRoom.controller;

import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomRequest;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/room")
@PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN', 'OWNER')")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@PathVariable Long hotelId,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long hotelId,@PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.updateRoom(hotelId,roomId, request);
        return ResponseEntity.ok(response);
    }



    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long roomId) {
        String response = roomService.deleteRoom(roomId);
        return ResponseEntity.ok(response);
    }


}
