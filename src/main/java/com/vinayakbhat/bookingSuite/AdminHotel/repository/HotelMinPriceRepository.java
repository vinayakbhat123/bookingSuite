package com.vinayakbhat.bookingSuite.AdminHotel.repository;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.HotelMinPrice;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceDto;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelResponse;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {
    @Query("""
        SELECT 
            h.id AS hotelId,
            h.hotelName AS hotelName,
            h.cityName AS cityName,
            MIN(mp.price) AS price
        FROM HotelMinPrice mp 
        JOIN mp.hotel h 
        WHERE (:city IS NULL OR :city = '' OR LOWER(:city) = 'all' OR LOWER(h.cityName) = LOWER(:city)) 
          AND h.active = true 
          AND mp.date BETWEEN :startDate AND :endDate 
        GROUP BY h.id, h.hotelName, h.cityName
    """)
    Page<HotelMinPriceProjection> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );





    List<HotelMinPrice> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);
}


