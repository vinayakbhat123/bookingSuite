package com.vinayakbhat.bookingSuite.Inventory.dto;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        LocalDate date,
        Integer bookedCount,
        Integer reservedCount,
        Integer totalCount,
        BigDecimal surgeFactor,
        BigDecimal price,
        Boolean closed,
        String city,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
