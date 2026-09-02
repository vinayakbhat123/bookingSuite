package com.vinayakbhat.bookingSuite.Booking.controller;

import com.vinayakbhat.bookingSuite.Booking.dto.GuestRequest;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingRequest;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.Service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<BookingResponse>> initBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(ApiResponse.success(bookingService.initiateBooking(bookingRequest)));
    }
    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<ApiResponse<BookingResponse>> addGuest(@PathVariable Long bookingId,
                                                                 @RequestBody List<GuestRequest> guestRequests){
        return ResponseEntity.ok(ApiResponse.success(bookingService.addGuests(bookingId,guestRequests)));
    }
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<ApiResponse<String>> initiatePayment(@PathVariable Long bookingId){
        String sessionUrl = bookingService.initiatePayment(bookingId);
        return ResponseEntity.ok(ApiResponse.success(sessionUrl));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelPayment(@PathVariable Long bookingId) {
        String message = bookingService.canclePayment(bookingId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(bookingId)));
    }

    @PostMapping("/{bookingId}/payments/verify")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyPayment(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.verifyPayment(bookingId, sessionId)));
    }
}
