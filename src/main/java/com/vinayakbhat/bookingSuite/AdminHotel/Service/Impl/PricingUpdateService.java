package com.vinayakbhat.bookingSuite.AdminHotel.Service.Impl;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.HotelMinPrice;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelMinPriceRepository;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.Booking.Strategy.Impl.PricingService;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PricingUpdateService {
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final HotelMinPriceRepository hotelMinPriceRepository;

    public PricingUpdateService(HotelRepository hotelRepository, InventoryRepository inventoryRepository, PricingService pricingService, HotelMinPriceRepository hotelMinPriceRepository) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.pricingService = pricingService;
        this.hotelMinPriceRepository = hotelMinPriceRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void updatePrices() {
        log.info("Update prices started");

        int page = 0;
        int pageSize = 100;
        while (true) {
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, pageSize));
            if (hotelPage.getContent().isEmpty()) {
                break;
            }
            hotelPage.getContent().forEach(this::updateHotelPrice);
            page++;
        }
    }

    public void updateHotelPrice(Hotel hotel) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);

        updateInventoryPrice(inventoryList);
        updateHotelMinPrice(hotel, inventoryList, startDate, endDate);
    }

    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate) {
        // 1. Group and find min price
        Map<LocalDate, BigDecimal> minPricePerDay = inventoryList.stream()
                .filter(inv -> inv.getPrice() != null)
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(
                                Inventory::getPrice,
                                Collectors.minBy(Comparator.naturalOrder())
                        )
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        // 2. Fetch existing in ONE query (O(1) DB calls instead of O(N))
        Map<LocalDate, HotelMinPrice> existingPriceMap = hotelMinPriceRepository
                .findByHotelAndDateBetween(hotel, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(HotelMinPrice::getDate, Function.identity()));

        // 3. Prepare Bulk Updates
        List<HotelMinPrice> hotelMinPricesToSave = new ArrayList<>();

        minPricePerDay.forEach((date, minPrice) -> {
            // Check memory Map instead of hitting the database
            HotelMinPrice minPriceEntity = existingPriceMap.computeIfAbsent(date, k -> {
                HotelMinPrice newEntity = new HotelMinPrice();
                newEntity.setHotel(hotel);
                newEntity.setDate(k);
                return newEntity;
            });

            minPriceEntity.setPrice(minPrice);
            hotelMinPricesToSave.add(minPriceEntity);
        });

        // 4. Save in Bulk
        hotelMinPriceRepository.saveAll(hotelMinPricesToSave);
    }

    private void updateInventoryPrice(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventoryList);
    }
}


