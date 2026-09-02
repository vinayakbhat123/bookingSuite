package com.vinayakbhat.bookingSuite.Booking.Strategy.Impl;

import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    public BigDecimal calculateDynamicPricing(Inventory inventory){
        PricingStrategy pricingStrategy = new BasePricingStrategy(inventory);
        // Apply the all Strategies
        pricingStrategy = new SurgePricingStrategy(pricingStrategy);
        pricingStrategy = new OccupancyPricingStrategy(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        return pricingStrategy.calculatePrice(inventory);
    }
}
