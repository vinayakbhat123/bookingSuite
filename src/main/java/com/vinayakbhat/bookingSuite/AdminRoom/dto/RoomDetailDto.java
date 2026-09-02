package com.vinayakbhat.bookingSuite.AdminRoom.dto;

import java.math.BigDecimal;
import java.util.List;


public record RoomDetailDto(
    Long roomId,
    String roomType,
    BigDecimal basePrice,
    int capacity,
    int floor,
    List<String> photos,
    List<String> amenities
) {}