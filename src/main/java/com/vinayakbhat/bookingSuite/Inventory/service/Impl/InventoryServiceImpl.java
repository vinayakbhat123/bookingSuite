package com.vinayakbhat.bookingSuite.Inventory.service.Impl;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceDto;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceProjection;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelSearchResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelMinPriceProjection;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelMinPriceRepository;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.dto.UpdateInventoryRequest;
import com.vinayakbhat.bookingSuite.Mapper.HotelMapper;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelSearchRequest;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.Inventory.service.InventoryService;
import com.vinayakbhat.bookingSuite.Mapper.InventoryMapper;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final InventoryMapper inventoryMapper;
    private final HotelMapper hotelMapper;

    @Override
    @Transactional
    // REDIS CACHE FIX: Deletes any stale cache for this room's inventory so the fresh 365 days are visible immediately.
    @CacheEvict(value = "inventory", key = "#room.id")
    public void initializeRoomForAYear(Room room) {
        if (room == null || room.getId() == null) {
            throw new IllegalArgumentException("Cannot initialize inventory for unpersisted room");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        log.info("Initializing inventory batch for room ID: {} from {} to {}", room.getId(), startDate, endDate);

        List<Inventory> inventoryList = new ArrayList<>(365);

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            Inventory inventory = new Inventory();
            inventory.setRoom(room);
            inventory.setHotel(room.getHotel());
            inventory.setDate(date);
            inventory.setBookedCount(0);
            inventory.setReservedCount(0);
            inventory.setSurgeFactor(BigDecimal.ONE);
            inventory.setClosed(false);
            inventory.setCity(room.getHotel().getCityName());
            inventory.setTotalCount(room.getTotalCount());
            inventory.setPrice(room.getBasePrice());

            inventoryList.add(inventory);
        }

        // High-concurrency bulk save (Ensures batch insert if configured in application.yml)
        inventoryRepository.saveAll(inventoryList);
        log.info("Successfully initialized 365 days of inventory for room ID: {}", room.getId());
    }

    @Override
    @Transactional
    // REDIS CACHE FIX: Immediately removes this room's inventory from Redis.
    // This prevents the frontend from showing ghost inventory that no longer exists in PostgreSQL.
    @CacheEvict(value = "inventory", key = "#room.id")
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelSearchResponse searchHotels(HotelSearchRequest request) {
        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());

        // 1. Fetch the paginated projection results from the repository
        Page<HotelMinPriceProjection> projectionPage = hotelMinPriceRepository.findHotelsWithAvailableInventory(
                request.city(),
                request.startDate(),
                request.endDate(),
                pageable
        );

        if (projectionPage.isEmpty()) {
            return new HotelSearchResponse(List.of(), projectionPage.getNumber(), projectionPage.getSize(), 0, 0);
        }

        // 2. Extract hotel IDs to fetch photos and amenities efficiently
        List<Long> hotelIds = projectionPage.getContent().stream()
                .map(HotelMinPriceProjection::getHotelId)
                .toList();

        // 3. Map hotel IDs to their entities to retrieve collections safely
        Map<Long, Hotel> hotelMap = hotelRepository.findAllById(hotelIds).stream()
                .collect(Collectors.toMap(Hotel::getId, h -> h));

        // 4. Construct your 6-field HotelPriceDto record
        List<HotelPriceDto> content = projectionPage.getContent().stream().map(row -> {
            Hotel hotel = hotelMap.get(row.getHotelId());
            return new HotelPriceDto(
                    row.getHotelId(),
                    row.getHotelName(),
                    row.getCityName(),
                    hotel != null ? hotel.getPhotos() : List.of(),
                    hotel != null ? hotel.getAmenities() : List.of(),
                    row.getPrice()
            );
        }).toList();

        // 5. Wrap inside the final paginated response container
        return new HotelSearchResponse(
                content,
                projectionPage.getNumber(),
                projectionPage.getSize(),
                projectionPage.getTotalElements(),
                projectionPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    // REDIS CACHE FIX: Caches the inventory list by Room ID.
    @Cacheable(value = "roomInventory", key = "#roomId")
    public List<InventoryResponse> getInventoryByRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found"));
        User user = getCurrentUser();
        assert user != null;
        if(!user.equals(room.getHotel().getOwner())) {
            throw  new AccessDeniedException("This User Not Owner of this Hotel of Id: ");
        }
        return inventoryRepository.findByRoomOrderByDate(room)
                .stream()
                .map(inventoryMapper:: toInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    // REDIS CACHE FIX: Deletes the stale inventory cache for this room.
    // The next time the owner views their inventory, it will pull the fresh surge prices and closed statuses from PostgreSQL.
    @CacheEvict(value = "roomInventory", key = "#roomId")
    public String updateInventory(Long roomId, UpdateInventoryRequest request) {
        log.info("Updating inventory for room Id: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found"));
        User user = getCurrentUser();
        assert user != null;
        if(!user.equals(room.getHotel().getOwner())) {
            throw  new AccessDeniedException("This User Not Owner of this Hotel of Id: ");
        }
        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId,request.startDate(),request.endDate());
        inventoryRepository.updateInventory(roomId,request.startDate(),request.endDate(),request.closed(),request.surgeFactor());
        log.info("Successfully updated inventory for room Id: {}", roomId);
        return "Successfully updated inventory for room Id: {}\", roomId";
    }

    private User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}