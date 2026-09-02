package com.vinayakbhat.bookingSuite.AdminHotel.Entity;

import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "hotel",
        indexes = {
                @Index(name = "idx_hotel_city", columnList = "city_name"),
                @Index(name = "idx_hotel_name", columnList = "hotel_name"),
                @Index(name = "idx_hotel_owner", columnList = "owner_id")
        }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prevents Lost Updates in high-concurrency environments
    @Version
    private Long version;

    @Column(name = "hotel_name", nullable = false,unique = true)
    private String hotelName;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    // Modern Hibernate 6 mapping for Database Arrays to Java Lists
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photos", columnDefinition = "text[]")
    private List<String> photos = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "amenities", columnDefinition = "text[]")
    private List<String> amenities = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = false;

    @Embedded
    private HotelContactInfo contactInfo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(
            mappedBy = "hotel",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Room> rooms = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Required by JPA
    public Hotel() {
    }

    public Hotel(String hotelName, String cityName, HotelContactInfo contactInfo, User owner) {
        this.hotelName = Objects.requireNonNull(hotelName, "hotelName must not be null");
        this.cityName = Objects.requireNonNull(cityName, "cityName must not be null");
        this.contactInfo = Objects.requireNonNull(contactInfo, "contactInfo must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.active = false;
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

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? amenities : new ArrayList<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public HotelContactInfo getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(HotelContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms != null ? rooms : new ArrayList<>();
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

    // --- BIDIRECTIONAL UTILITY METHODS (Critical for Performance) ---

    public void addRoom(Room room) {
        rooms.add(room);
        room.setHotel(this); // Syncs the foreign key side
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setHotel(null); // Triggers orphan removal safely
    }

    // --- JPA EQUALS & HASHCODE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotel hotel)) return false;
        return id != null && id.equals(hotel.getId());
    }

    @Override
    public int hashCode() {
        // Must remain constant across all entity state transitions
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Hotel{" +
                "id=" + id +
                ", hotelName='" + hotelName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", active=" + active +
                '}';
    }
}