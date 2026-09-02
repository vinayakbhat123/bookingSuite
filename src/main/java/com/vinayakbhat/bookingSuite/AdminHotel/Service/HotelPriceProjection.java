package com.vinayakbhat.bookingSuite.AdminHotel.Service;

import java.math.BigDecimal;

public interface HotelPriceProjection {
    Long getHotelId();
    String getHotelName();
    String getCityName();
    BigDecimal getPrice();
}