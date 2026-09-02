package com.vinayakbhat.bookingSuite.AdminHotel.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "hotel_min_price",
        indexes = {
                // 1. Range Query Index: "Show me prices for Hotel X from Oct 1 to Oct 5"
                @Index(name = "idx_hotel_date", columnList = "hotel_id, date"),
                // 2. Search & Sort Index: "Show me all hotels on Oct 1, sorted by lowest price"
                @Index(name = "idx_date_price", columnList = "date, price")
        },
        uniqueConstraints = {
                // 3. Data Integrity: A hotel cannot have two different "minimum" prices for the exact same day
                @UniqueConstraint(name = "uk_hotel_date", columnNames = {"hotel_id", "date"})
        }
)
public class HotelMinPrice {

    @Id
    // Optimized for JDBC Batch Inserts (Critical for creating 365 days of prices at once)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hmp_seq")
    @SequenceGenerator(name = "hmp_seq", sequenceName = "hotel_min_price_seq", allocationSize = 50)
    private Long id;

    // Prevents concurrent price update collisions
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Required by JPA
    public HotelMinPrice() {
    }

    public HotelMinPrice(Hotel hotel, LocalDate date, BigDecimal price) {
        this.hotel = Objects.requireNonNull(hotel, "hotel must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.price = Objects.requireNonNull(price, "price must not be null");
    }

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    // --- JPA EQUALS & HASHCODE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HotelMinPrice that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}