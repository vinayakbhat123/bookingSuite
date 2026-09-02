package com.vinayakbhat.bookingSuite.AdminRoom.service.Impl;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import com.vinayakbhat.bookingSuite.Mapper.RoomMapper;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomRequest;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Inventory.service.InventoryService;
import com.vinayakbhat.bookingSuite.AdminRoom.service.RoomService;
import com.vinayakbhat.bookingSuite.common.security.Exception.UnAuthorisedException;
import com.vinayakbhat.bookingSuite.util.RoomNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final InventoryService  inventoryService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final InventoryRepository inventoryRepository;

    public RoomServiceImpl(HotelRepository hotelRepository, RoomRepository roomRepository, RoomMapper roomMapper, InventoryService inventoryService, InventoryRepository inventoryRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotelRooms", key = "#hotelId")
    public RoomResponse createRoom(Long hotelId, RoomRequest request) {
        log.info("Create Room  in hotel: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("hotel Not Found on this Id: " + hotelId));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new UnAuthorisedException("This User does not Own this Hotel of Id: "+hotelId);
        }

        Room room = roomMapper.fromRequestToRoom(request);
        String generatedRoomNumber = RoomNumberGenerator.generateRoomNumber(request.floor());
        room.setRoomNumber(generatedRoomNumber);
        room.setHotel(hotel);
        Room savedroom = roomRepository.save(room);
        if(hotel.isActive()) {
            inventoryService.initializeRoomForAYear(savedroom);
        }

        log.info("Room created in hotel: {}", hotelId);
        return roomMapper.toRoomResponse(savedroom);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"room", "hotelRooms"}, allEntries = true)
    public RoomResponse updateRoom(Long hotelId,Long roomId, RoomRequest request) {
        log.info("Update Room in hotel: {}", roomId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("hotel Not Found on this Id: " + hotelId));

        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new AccessDeniedException("This User does not Own this room with this Id: "+roomId);
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found on this Id: " + roomId));

        // MapStruct mutates the existing managed 'room' instance safely
        roomMapper.updateRoomFromDto(request, room);
        room.setId(roomId);
        room.setRoomNumber(RoomNumberGenerator.generateRoomNumber(request.floor()));
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toRoomResponse(savedRoom);
    }

    // public api
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "room", key = "#roomId")
    public RoomResponse getRoom(Long hotelId,Long roomId) {
        log.info("Get Room in hotel: {}", roomId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found on this Id: " + hotelId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found on this Id: " + roomId));
        return roomMapper.toRoomResponse(room);
    }

    // public api
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hotelRooms", key = "#hotelId")
    public List<RoomResponse> getAllHotelRooms(Long hotelId) {
        log.info("Get All Room in hotel: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found on this Id: " + hotelId));
        List<Room> rooms = hotel.getRooms();
        return rooms.stream().map(roomMapper::toRoomResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"room", "hotelRooms"}, allEntries = true)
    public String deleteRoom(Long roomId) {
        log.info("Delete Room in hotel: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found on this Id: " + roomId));
        User user = getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())) {
            throw  new UnAuthorisedException("This User does not Own this room with this Id: "+ roomId);
        }
        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);
        return "Room has been deleted: " + roomId;
    }

    private User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}
