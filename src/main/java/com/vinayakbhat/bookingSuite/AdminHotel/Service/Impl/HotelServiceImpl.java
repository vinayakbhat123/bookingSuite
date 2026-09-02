package com.vinayakbhat.bookingSuite.AdminHotel.Service.Impl;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelDetailResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomDetailDto;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import com.vinayakbhat.bookingSuite.Mapper.HotelMapper;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelInfoResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;
import com.vinayakbhat.bookingSuite.AdminRoom.dto.RoomResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.AdminHotel.Service.HotelService;
import com.vinayakbhat.bookingSuite.Inventory.service.InventoryService;
import com.vinayakbhat.bookingSuite.common.security.Exception.UnAuthorisedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@Transactional(readOnly = true)
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingUpdateService pricingUpdateService;

    public HotelServiceImpl(HotelRepository hotelRepository, HotelMapper hotelMapper, InventoryService inventoryService, RoomRepository roomRepository, UserRepository userRepository, InventoryRepository inventoryRepository, PricingUpdateService pricingUpdateService) {
        this.hotelRepository = hotelRepository;
        this.hotelMapper = hotelMapper;
        this.inventoryService = inventoryService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.pricingUpdateService = pricingUpdateService;
    }

    @Override
    @Transactional
    @CacheEvict(value = "allHotels", allEntries = true)
    @PreAuthorize("hasAnyRole('OWNER', 'HOTEL_MANAGER', 'ADMIN')")
    public HotelRequest createHotel(HotelRequest hotelRequest) {
        log.info("creating  New Hotel with name: {}", hotelRequest.hotelName());
        Hotel hotel = hotelMapper.toHotel(hotelRequest);
        User user = getCurrentUser();
        User mockGuest = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        hotel.setOwner(mockGuest);
        hotel.setActive(false);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("created New Hotel with id: {}", savedHotel.getId());
        return hotelMapper.toHotelRequest(savedHotel);
    }

    // Public Api
    @Override
    @Transactional(readOnly = true)
    public HotelRequest getHotelById(Long id) {
        log.info("getting Hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return hotelMapper.toHotelRequest(hotel);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "hotelInfo", key = "#id"),
            @CacheEvict(value = "allHotels", allEntries = true)
    })
    @PreAuthorize("hasAnyRole('OWNER', 'HOTEL_MANAGER', 'ADMIN')")
    public HotelRequest updateHotel(Long id, HotelRequest request) {
        log.info("Updating hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new UnAuthorisedException("This User Not Owner of this Hotel of Id: "+id);
        }

        if (request.hotelName() != null && !request.hotelName().isBlank()) {
            hotel.setHotelName(request.hotelName().trim());
        }
        if (request.cityName() != null && !request.cityName().isBlank()) {
            hotel.setCityName(request.cityName().trim());
        }
        if (request.photos() != null) {
            hotel.setPhotos(List.of(request.photos()));
        }
        if (request.amenities() != null) {
            hotel.setAmenities(List.of(request.amenities()));
        }
        if (request.contactInfo() != null) {
            hotel.setContactInfo(new com.vinayakbhat.bookingSuite.AdminHotel.Entity.HotelContactInfo(
                    request.contactInfo().address(),
                    request.contactInfo().phoneNumber(),
                    request.contactInfo().email(),
                    request.contactInfo().location()
            ));
        }

        Hotel saved = hotelRepository.save(hotel);
        return hotelMapper.toHotelRequest(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "hotelInfo", key = "#id"),
            @CacheEvict(value = "allHotels", allEntries = true)
    })
    @PreAuthorize("hasAnyRole('OWNER', 'HOTEL_MANAGER', 'ADMIN')")
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new UnAuthorisedException("This User Not Owner of this Hotel of Id: "+id);
        }

        for(Room room: hotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());}
        hotelRepository.deleteById(id);
        log.info("deleted Hotel with id: {}", id);
    }

    // public api
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "allHotels")
    public List<HotelRequest> getAllHotel() {
        log.info("getting All hotel");
        return hotelRepository.findAll()
                .stream()
                .map(hotelMapper::toHotelRequest)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "hotelInfo", key = "#id"),
            @CacheEvict(value = "allHotels", allEntries = true)
    })
    @PreAuthorize("hasAnyRole('OWNER', 'HOTEL_MANAGER', 'ADMIN')")
    public String activateHotel(Long id) {
        log.info("activating  Hotel By  Id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new UnAuthorisedException("This User Not Owner of this Hotel of Id: "+id);
        }

        hotel.setActive(true);
        Hotel savedHotel = hotelRepository.save(hotel);
        for (Room room : savedHotel.getRooms()) {
            inventoryService.initializeRoomForAYear(room);
        }

        try {
            pricingUpdateService.updateHotelPrice(savedHotel);
        } catch (Exception e) {
            log.warn("Could not immediately recalculate min price for activated hotel {}: {}", id, e.getMessage());
        }

        log.info("Successfully activated  Hotel Id:{}", id);
        return "Hotel activated Successfully";
    }

    @Override
    @CacheEvict(value = "hotelInfo", key = "#id")
    @PreAuthorize("hasAnyRole('OWNER', 'HOTEL_MANAGER', 'ADMIN')")

    public String deactivateHotel(Long id) {
        log.info("Deactivating  Hotel By  Id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw  new AccessDeniedException("This User Not Owner of this Hotel of Id: "+id);
        }

        hotel.setActive(false);
        log.info("Successfully Deactivated  Hotel Id:{}", id);
        return "Hotel deactivated Successfully";
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hotelInfo", key = "#hotelId")
    public HotelInfoResponse getHotelInfoById(Long hotelId) {
        log.info("getting Hotel By Id: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));
        List<RoomResponse> rooms = roomRepository.findByHotelId(hotelId)
                .stream()
                .map(hotelMapper::toRoomResponse)
                .toList();
        return new HotelInfoResponse(hotelMapper.toHotelResponse(hotel), rooms);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelRequest> getAllHotelOfOwner() {
        log.info("getting All hotel for owner");
        User user = getCurrentUser();
        return hotelRepository.findByOwnerId(user.getId())
                .stream()
                .map(hotelMapper::toHotelRequest)
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelDetailResponse getHotelDetailsWithPricing(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        BigDecimal totalPrice = inventoryRepository.calculateStayPriceForHotel(hotelId, startDate, endDate);

        List<RoomDetailDto> roomDtos = rooms.stream().map(room -> new RoomDetailDto(
                room.getId(),
                room.getRoomType().name(),
                room.getBasePrice(),
                room.getCapacity(),
                room.getFloor(),
                room.getPhotos(),
                room.getAmenities()
        )).toList();

        return new HotelDetailResponse(
                hotel.getId(),
                hotel.getHotelName(),
                hotel.getCityName(),
                hotel.getContactInfo().getAddress(),
                hotel.getPhotos(),
                hotel.getAmenities(),
                totalPrice != null ? totalPrice : BigDecimal.ZERO,
                roomDtos
        );
    }

    private User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}