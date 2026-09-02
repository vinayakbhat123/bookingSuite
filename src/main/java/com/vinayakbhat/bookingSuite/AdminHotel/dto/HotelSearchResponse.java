package com.vinayakbhat.bookingSuite.AdminHotel.dto;

import java.io.Serializable;
import java.util.List;

public record HotelSearchResponse(
        List<HotelPriceDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) implements Serializable {}