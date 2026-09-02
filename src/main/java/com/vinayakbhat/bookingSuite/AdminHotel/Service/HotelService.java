package com.vinayakbhat.bookingSuite.AdminHotel.Service;

import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelDetailResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelInfoResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;

import java.time.LocalDate;
import java.util.List;

public interface HotelService {
    HotelRequest createHotel(HotelRequest hotel);

    HotelRequest getHotelById(Long id);

    HotelRequest updateHotel(Long id, HotelRequest request);

    void deleteHotel(Long id);

    List<HotelRequest> getAllHotel();

    String activateHotel(Long id);
    String deactivateHotel(Long id);

    HotelInfoResponse getHotelInfoById(Long hotelId);

    List<HotelRequest> getAllHotelOfOwner();

    HotelDetailResponse getHotelDetailsWithPricing(Long hotelId, LocalDate startDate, LocalDate endDate);
}







