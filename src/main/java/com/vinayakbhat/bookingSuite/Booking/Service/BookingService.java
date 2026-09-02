package com.vinayakbhat.bookingSuite.Booking.Service;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelReport;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingRequest;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.GuestRequest;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    BookingResponse initiateBooking(BookingRequest bookingRequest);

    BookingResponse addGuests(Long bookingId, List<GuestRequest> guestRequests);
    String initiatePayment(Long bookingId);

    String canclePayment(Long bookingId);

    List<BookingResponse> getAllBookingsByHotelId(Long hotelId);

    HotelReport getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate);

    BookingResponse getBookingById(Long bookingId);

    BookingResponse verifyPayment(Long bookingId, String sessionId);
}




