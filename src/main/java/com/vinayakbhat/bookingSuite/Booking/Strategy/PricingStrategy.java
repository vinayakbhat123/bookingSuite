package com.vinayakbhat.bookingSuite.Booking.Strategy;

import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculatePrice(Inventory inventory);
}
