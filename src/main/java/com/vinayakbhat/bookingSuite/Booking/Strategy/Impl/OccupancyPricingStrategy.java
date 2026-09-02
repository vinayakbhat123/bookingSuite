package com.vinayakbhat.bookingSuite.Booking.Strategy.Impl;

import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;



public class OccupancyPricingStrategy implements PricingStrategy
{
    private final PricingStrategy wrapper;

    public OccupancyPricingStrategy(PricingStrategy wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapper.calculatePrice(inventory);
        if (price == null) throw new IllegalArgumentException("Base price cannot be null");

        double occupancyRate = (double) inventory.getBookedCount() / inventory.getTotalCount();
        BigDecimal occupancyFactor = BigDecimal.ONE;
        if (occupancyRate >= 0.90) {
            occupancyFactor = new BigDecimal("1.50");
        } else if (occupancyRate >= 0.75) {
            occupancyFactor = new BigDecimal("1.30");
        } else if (occupancyRate >= 0.50) {
            occupancyFactor = new BigDecimal("1.15");
        }
        return price.multiply(occupancyFactor);
    }
}
