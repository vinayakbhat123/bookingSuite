package com.vinayakbhat.bookingSuite.Booking.Entity;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Booking.Enum.BookingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_hotel", columnList = "hotel_id"),
                @Index(name = "idx_booking_user", columnList = "user_id"),
                // Dedicated index for Stripe Webhook lookups
                @Index(name = "idx_booking_stripe", columnList = "stripe_session_id"),
                // CRITICAL: Composite index to make Room Availability checks lightning fast
                @Index(
                        name = "idx_booking_availability",
                        columnList = "room_id, status, check_in_date, check_out_date"
                )
        }
)
public class Booking {

    @Id
    // Optimized for JDBC Batching to handle high-traffic booking spikes
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "bookings_seq", allocationSize = 20)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "rooms_count", nullable = false)
    private Integer roomsCount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_guest",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "guest_id")
    )
    private Set<Guest> guests = new HashSet<>();

    // Optimistic locking to prevent double-processing of payments or state changes
    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "stripe_session_id", unique = true, length = 255)
    private String stripeSessionId;

    // Required by JPA
    public Booking() {
    }

    public Booking(
            Hotel hotel,
            Room room,
            User user,
            BookingStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomsCount,
            BigDecimal price,
            String stripeSessionId
    ) {
        this.hotel = Objects.requireNonNull(hotel, "hotel must not be null");
        this.room = Objects.requireNonNull(room, "room must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.checkInDate = Objects.requireNonNull(checkInDate, "checkInDate must not be null");
        this.checkOutDate = Objects.requireNonNull(checkOutDate, "checkOutDate must not be null");

        // Domain Validation: Prevent impossible bookings
        if (!this.checkOutDate.isAfter(this.checkInDate)) {
            throw new IllegalArgumentException("checkOutDate must be strictly after checkInDate");
        }

        this.roomsCount = Objects.requireNonNull(roomsCount, "roomsCount must not be null");
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.stripeSessionId = stripeSessionId;
    }

    // --- GETTERS AND SETTERS ---

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
        this.hotel = hotel;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        if (checkOutDate != null && checkInDate != null && !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("checkOutDate must be strictly after checkInDate");
        }
        this.checkOutDate = checkOutDate;
    }

    public Integer getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(Integer roomsCount) {
        this.roomsCount = roomsCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public Set<Guest> getGuests() {
        return guests;
    }

    public void setGuests(Set<Guest> guests) {
        this.guests = guests != null ? guests : new HashSet<>();
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

    // --- COLLECTION SYNCHRONIZATION METHODS ---



    // --- JPA EQUALS & HASHCODE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id != null && id.equals(booking.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", status=" + status +
                ", checkInDate=" + checkInDate +
                ", checkOutDate=" + checkOutDate +
                ", roomsCount=" + roomsCount +
                ", price=" + price +
                '}';
    }
}