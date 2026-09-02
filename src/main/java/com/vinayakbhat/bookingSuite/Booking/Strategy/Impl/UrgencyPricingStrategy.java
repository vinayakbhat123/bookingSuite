package com.vinayakbhat.bookingSuite.Booking.Strategy.Impl;

import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;


public class UrgencyPricingStrategy implements PricingStrategy {
    private final PricingStrategy wrapper;

    public UrgencyPricingStrategy(PricingStrategy wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapper.calculatePrice(inventory);
        if (price == null) throw new IllegalArgumentException("Base price cannot be null");
        LocalDate today = LocalDate.now();
        if(inventory.getDate().isBefore(today.plusDays(7)) && !inventory.getDate().isBefore(today)){
            price = price.multiply(BigDecimal.valueOf(1.15));
        }
        return price;
    }
}
