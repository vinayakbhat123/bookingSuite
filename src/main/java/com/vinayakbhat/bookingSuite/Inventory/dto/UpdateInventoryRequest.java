package com.vinayakbhat.bookingSuite.Inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInventoryRequest(
        LocalDate startDate,
        LocalDate endDate,
        Boolean closed,
        BigDecimal surgeFactor
) {
}
