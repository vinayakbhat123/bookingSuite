package com.vinayakbhat.bookingSuite.Booking.Strategy.Impl;

import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Slf4j
public class SurgePricingStrategy implements PricingStrategy {
    private final PricingStrategy wrapper;


    public SurgePricingStrategy(PricingStrategy wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapper.calculatePrice(inventory);
        if (price == null) throw new IllegalArgumentException("Base price cannot be null");
        return price.multiply(inventory.getSurgeFactor());
    }
}
