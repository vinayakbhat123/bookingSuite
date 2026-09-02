package com.vinayakbhat.bookingSuite.Inventory.repository;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceDto;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelPriceProjection;
import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Modifying
    @Transactional
    void deleteByRoom(Room room);

    @Query("""
    SELECT 
        h.id AS hotelId,
        h.hotelName AS hotelName,
        h.cityName AS cityName,
        SUM(i.price * i.surgeFactor) AS price
    FROM Inventory i
    JOIN i.hotel h
    WHERE LOWER(h.cityName) = LOWER(:city)
      AND i.date >= :startDate
      AND i.date < :endDate
      AND i.closed = false
      AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
    GROUP BY h.id, h.hotelName, h.cityName
    HAVING COUNT(DISTINCT i.date) = :totalDays
    ORDER BY SUM(i.price * i.surgeFactor) ASC
""")
    Page<HotelPriceProjection> searchHotelsByInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("totalDays") Long totalDays,
            Pageable pageable
    );

    @Query("""
    SELECT 
        h.id AS hotelId,
        h.hotelName AS hotelName,
        h.cityName AS cityName,
        SUM(i.price * i.surgeFactor) AS price
    FROM Inventory i
    JOIN i.hotel h
    WHERE LOWER(h.cityName) = LOWER(:city)
      AND i.date >= :startDate
      AND i.date < :endDate
      AND i.closed = false
      AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
    GROUP BY h.id, h.hotelName, h.cityName
    HAVING COUNT(DISTINCT i.date) = :totalDays
    ORDER BY SUM(i.price * i.surgeFactor) ASC
""")
    Page<HotelPriceProjection> searchHotelsWithProjection(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("totalDays") Long totalDays,
            Pageable pageable
    );

    @Query("""
    SELECT SUM(i.price * i.surgeFactor)
    FROM Inventory i
    WHERE i.hotel.id = :hotelId
      AND i.date >= :startDate
      AND i.date < :endDate
      AND i.closed = false
""")
    BigDecimal calculateStayPriceForHotel(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT i FROM Inventory i
    WHERE i.room.id = :roomId
      AND i.date BETWEEN :startDate AND :endDate
    ORDER BY i.date ASC
""")
    List<Inventory> getRoomCalendarGrid(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.room.id = :roomId
          AND i.date >= :startDate AND i.date < :endDate
          AND i.closed = false
          AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
        ORDER BY i.date ASC
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount
    );

    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.room.id = :roomId
          AND i.date >= :startDate AND i.date < :endDate
          AND i.closed = false
          AND (i.totalCount - i.bookedCount) >= :numberOfRooms
        ORDER BY i.date ASC
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    List<Inventory> findAvailableInventoryForUpdate(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("numberOfRooms") Integer numberOfRooms
    );

    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.reservedCount = i.reservedCount + :numberOfRooms
        WHERE i.room.id = :roomId
          AND i.date >= :startDate AND i.date < :endDate
          AND i.closed = false
          AND (i.totalCount - i.bookedCount - i.reservedCount) >= :numberOfRooms
    """)
    int initBooking(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("numberOfRooms") Integer numberOfRooms
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.reservedCount = CASE WHEN i.reservedCount >= :numberOfRooms THEN i.reservedCount - :numberOfRooms ELSE 0 END,
            i.bookedCount = i.bookedCount + :numberOfRooms
        WHERE i.room.id = :roomId
          AND i.date >= :startDate AND i.date < :endDate
          AND (i.totalCount - i.bookedCount) >= :numberOfRooms
          AND i.closed = false
    """)
    int confirmReservedBooking(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate checkInDate,
            @Param("endDate") LocalDate checkOutDate,
            @Param("numberOfRooms") Integer numberOfRooms
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.bookedCount = CASE WHEN i.bookedCount >= :numberOfRooms THEN i.bookedCount - :numberOfRooms ELSE 0 END
        WHERE i.room.id = :roomId
          AND i.date >= :startDate AND i.date < :endDate
    """)
    int cancelReservedBooking(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate checkInDate,
            @Param("endDate") LocalDate checkOutDate,
            @Param("numberOfRooms") Integer numberOfRooms
    );

    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    List<Inventory> findByRoomOrderByDate(Room room);

    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> getInventoryAndLockBeforeUpdate(@Param("roomId") Long roomId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.surgeFactor = :surgeFactor,
                i.closed =:closed
                WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                """)
    void updateInventory(@Param("roomId") Long roomId,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate,
                         @Param("closed")  boolean closed,
                         @Param("surgeFactor")BigDecimal surgeFactor);

}





