package com.vinayakbhat.bookingSuite.Booking.Entity;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "guest",
        indexes = {
                // Composite Index: Covers both "fetch all guests for a user"
                // AND "search/sort a user's guests by name" via the Leftmost Prefix Rule.
                @Index(name = "idx_guest_user_name", columnList = "user_id, name")
        }
)
public class Guest {

    @Id
    // Optimized for JDBC Batching (vital when inserting multiple guests for a single booking)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guest_seq")
    @SequenceGenerator(name = "guest_seq", sequenceName = "guest_seq", allocationSize = 20)
    private Long id;

    // Prevents concurrent update anomalies
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false)
    private Integer age;

    // --- The Missing Inverse Relationship ---
    // Mapped by the "guests" field in the Booking entity
    @ManyToMany(mappedBy = "guests", fetch = FetchType.LAZY)
    private Set<Booking> bookings = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ------------------- Constructors -------------------

    protected Guest() {
        // required by JPA
    }

    public Guest(User user, String name, Gender gender, Integer age) {
        this.setUser(user);
        this.setName(name);
        this.setGender(gender);
        this.setAge(age);
    }

    // ------------------- Getters & Setters -------------------

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        this.name = name.trim();
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = Objects.requireNonNull(gender, "gender must not be null");
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        Objects.requireNonNull(age, "age must not be null");
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("age must be a valid number between 0 and 150");
        }
        this.age = age;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(Set<Booking> bookings) {
        this.bookings = bookings != null ? bookings : new HashSet<>();
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

    // ------------------- equals / hashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest guest)) return false;
        return id != null && id.equals(guest.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Guest{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                '}';
    }
}