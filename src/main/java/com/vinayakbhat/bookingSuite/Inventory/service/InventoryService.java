package com.vinayakbhat.bookingSuite.Inventory.service;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceDto;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelSearchRequest;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelSearchResponse;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.dto.UpdateInventoryRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {
    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    HotelSearchResponse searchHotels(HotelSearchRequest hotelSearchRequest);

    List<InventoryResponse> getInventoryByRoom(Long roomId);

    String updateInventory(Long roomId, UpdateInventoryRequest request);
}


