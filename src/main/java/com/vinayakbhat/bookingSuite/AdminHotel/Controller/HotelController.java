package com.vinayakbhat.bookingSuite.AdminHotel.Controller;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelReport;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;
import com.vinayakbhat.bookingSuite.AdminHotel.Service.HotelService;
import com.vinayakbhat.bookingSuite.Booking.Service.BookingService;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
@PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN', 'OWNER')")
@Slf4j
public class HotelController {
    private final HotelService hotelService;
    private final BookingService bookingService;
    public HotelController(HotelService hotelService, BookingService bookingService) {
        this.hotelService = hotelService;
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<HotelRequest> createHotel(@Valid @RequestBody HotelRequest hotel) {
        return new ResponseEntity<>(hotelService.createHotel(hotel),HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelRequest> updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        log.info("REST request to update Hotel ID: {}", id);
        return new  ResponseEntity<>(hotelService.updateHotel(id,request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
    // Optional
    @PatchMapping("/{id}/activate")
    public ResponseEntity<String> activateHotel(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.activateHotel(id));
    }
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateHotel(@PathVariable Long id) {
        log.info("REST request to change hotel status: id={}", id);
        return ResponseEntity.ok(hotelService.deactivateHotel(id));
    }
    @GetMapping("/owner")
    public ResponseEntity<List<HotelRequest>> getAllHotelByOwner() {
        return new ResponseEntity<>(hotelService.getAllHotelOfOwner(), HttpStatus.OK);
    }
    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingResponse>> getAllBookingsByHotelId(@PathVariable Long hotelId) {
        return new ResponseEntity<>(bookingService.getAllBookingsByHotelId(hotelId), HttpStatus.OK);
    }
    @GetMapping("/{hotelId}/report")
    public ResponseEntity<HotelReport> getHotelReport(@PathVariable Long hotelId, @RequestParam(required = false)LocalDate startDate, @RequestParam(required = false)LocalDate endDate) {
        if(startDate == null) startDate = LocalDate.now().minusMonths(1);
        if(endDate == null) endDate = LocalDate.now();
        return new ResponseEntity<>(bookingService.getHotelReport(hotelId,startDate,endDate), HttpStatus.OK);
    }
}
