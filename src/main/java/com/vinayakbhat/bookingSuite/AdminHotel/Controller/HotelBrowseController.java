package com.vinayakbhat.bookingSuite.AdminHotel.Controller;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.*;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.Service.HotelService;
import com.vinayakbhat.bookingSuite.Inventory.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelBrowseController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;

    public HotelBrowseController(InventoryService inventoryService, HotelService hotelService) {
        this.inventoryService = inventoryService;
        this.hotelService = hotelService;
    }

    @PostMapping("/search")
    public ResponseEntity<HotelSearchResponse> searchHotels(@RequestBody  HotelSearchRequest hotelSearchRequest) {
        return ResponseEntity.ok(inventoryService.searchHotels(hotelSearchRequest));
    }
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelRequest> getHotelById(@PathVariable Long hotelId) {
        return new ResponseEntity<>(hotelService.getHotelById(hotelId), HttpStatus.OK);
    }
    @GetMapping("/{hotelId}/details")
    public ResponseEntity<HotelDetailResponse> getHotelDetails(
            @PathVariable Long hotelId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        HotelDetailResponse response = hotelService.getHotelDetailsWithPricing(hotelId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    // /hotels
    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoResponse> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
    @GetMapping("/allhotels")
    public ResponseEntity<List<HotelRequest>> getAllHotel() {
        return new ResponseEntity<>(hotelService.getAllHotel(), HttpStatus.OK);
    }
}
