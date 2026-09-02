package com.vinayakbhat.bookingSuite.AdminHotel.repository;

import java.math.BigDecimal;

public interface HotelMinPriceProjection {
    Long getHotelId();
    String getHotelName();
    String getCityName();
    BigDecimal getPrice();
}