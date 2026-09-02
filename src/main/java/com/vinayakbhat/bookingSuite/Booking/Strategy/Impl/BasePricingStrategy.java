package com.vinayakbhat.bookingSuite.Booking.Strategy.Impl;

import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
public class BasePricingStrategy implements PricingStrategy {
    public BasePricingStrategy(Inventory inventory) {
    }

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        Objects.requireNonNull(inventory, "Inventory must not be null");
        Objects.requireNonNull(inventory.getRoom(), () -> "Room must not be null. Inventory ID: " + inventory.getId());

        BigDecimal basePrice = inventory.getRoom().getBasePrice();

        if (basePrice == null) {
            throw new IllegalStateException(
                    "Room base price is null. Inventory ID: " + inventory.getId() + ", Room ID: " + inventory.getRoom().getId());
        }
        return basePrice;
    }
}
