package com.vinayakbhat.bookingSuite.Inventory.entity;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "inventory",
        indexes = {
                // 1. Core Search Index: "Find available rooms in a specific city for these dates"
                @Index(name = "idx_inventory_city_date_closed", columnList = "city, date, closed"),

                // 2. Hotel Detail Index: "Show availability for a specific hotel for these dates"
                @Index(name = "idx_inventory_hotel_date_closed", columnList = "hotel_id, date, closed"),

                // 3. Booking Engine Index: "Lock inventory for this exact room on these dates"
                @Index(name = "idx_inventory_room_date", columnList = "room_id, date")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hotel_room_date_city",
                columnNames = {"hotel_id", "room_id", "date", "city"}
        )
)
public class Inventory {

    @Id
    // Optimized for JDBC Batching (Critical for 365-day bulk inventory generation)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_seq")
    @SequenceGenerator(name = "inventory_seq", sequenceName = "inventory_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount = 0;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "surge_factor", precision = 5, scale = 2)
    private BigDecimal surgeFactor = BigDecimal.ONE;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean closed = false;

    @Column(nullable = false)
    private String city;

    // Critical for preventing overbooking when two users try to book the last room
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ------------------- Constructors -------------------

    // Required by JPA
    public Inventory() {
    }

    // Explicit All-Arguments Constructor
    public Inventory(
            Long id,
            Hotel hotel,
            Room room,
            LocalDate date,
            Integer bookedCount,
            Integer reservedCount,
            Integer totalCount,
            BigDecimal surgeFactor,
            BigDecimal price,
            Boolean closed,
            String city,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.setHotel(hotel);
        this.setRoom(room);
        this.setDate(date);
        this.setBookedCount(bookedCount);
        this.setReservedCount(reservedCount);
        this.setTotalCount(totalCount);
        this.setSurgeFactor(surgeFactor);
        this.setPrice(price);
        this.setClosed(closed);
        // City is intentionally overridden to guarantee sync with the Hotel entity
        this.city = hotel != null ? hotel.getCityName() : city;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Custom Convenience Constructor
    public Inventory(
            Hotel hotel,
            Room room,
            LocalDate date,
            Integer totalCount,
            BigDecimal surgeFactor,
            BigDecimal price,
            Boolean closed
    ) {
        this.setHotel(hotel);
        this.setRoom(room);
        this.setDate(date);
        this.setTotalCount(totalCount);
        this.setPrice(price);

        this.bookedCount = 0;
        this.reservedCount = 0;
        this.surgeFactor = surgeFactor != null ? surgeFactor : BigDecimal.ONE;
        this.closed = closed != null ? closed : false;
        // Denormalized sync
        this.city = hotel.getCityName();
    }

    // ------------------- Getters & Setters -------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = Objects.requireNonNull(hotel, "hotel must not be null");
        if (hotel.getCityName() != null) {
            this.city = hotel.getCityName();
        }
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = Objects.requireNonNull(room, "room must not be null");
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date, "date must not be null");
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Integer bookedCount) {
        this.bookedCount = bookedCount != null ? bookedCount : 0;
    }

    public Integer getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(Integer reservedCount) {
        this.reservedCount = reservedCount != null ? reservedCount : 0;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = Objects.requireNonNull(totalCount, "totalCount must not be null");
    }

    public BigDecimal getSurgeFactor() {
        return surgeFactor;
    }

    public void setSurgeFactor(BigDecimal surgeFactor) {
        this.surgeFactor = surgeFactor != null ? surgeFactor : BigDecimal.ONE;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed != null ? closed : false;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ------------------- Domain Logic -------------------

    /**
     * Calculates the available rooms safely in memory.
     */
    @Transient
    public int getAvailableCount() {
        return totalCount - (bookedCount + reservedCount);
    }

    // ------------------- equals / hashCode / toString -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory inventory)) return false;
        return id != null && id.equals(inventory.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", date=" + date +
                ", availableCount=" + getAvailableCount() +
                ", price=" + price +
                ", closed=" + closed +
                ", city='" + city + '\'' +
                '}';
    }
}