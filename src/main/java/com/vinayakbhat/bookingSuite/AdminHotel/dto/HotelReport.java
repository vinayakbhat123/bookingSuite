package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import java.math.BigDecimal;

public record HotelReport(
        Long TotalBooking,
        BigDecimal TotalRevenue,
        BigDecimal AverageRevenue
) {
}
