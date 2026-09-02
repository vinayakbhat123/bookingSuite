package com.vinayakbhat.bookingSuite.Auth.Entity;

import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Booking.Enum.Gender;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_app_user_phone", columnNames = "phone_number")
        },
        indexes = {
                @Index(name = "idx_app_user_email", columnList = "email"),
                @Index(name = "idx_app_user_phone", columnList = "phone_number"),
                @Index(name = "idx_app_user_name", columnList = "name, last_name")
        }
)
// Removed @DynamicUpdate to utilize JDBC Prepared Statement Caching
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // --- ARCHITECTURAL SHIFT ---
    // Stores roles as a PostgreSQL array (e.g., {"ADMIN", "GUEST"}) in a single column.
    // Eliminates the `user_roles` side-table and the expensive JOIN query entirely.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    // Standardized to Hibernate annotations instead of @PrePersist hooks
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User(String name, String email, String password, Set<Role> roles) {
        this.name = validateName(name);
        this.email = normalizeEmail(email);
        this.password = validatePassword(password);
        this.roles = roles == null || roles.isEmpty()
                ? new HashSet<>(Set.of(Role.GUEST))
                : new HashSet<>(roles);
    }

    // -------------------------------------------------------
    // Spring Security - UserDetails Contract (CRITICAL FIXES)
    // -------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPassword() {
        return password;
    }

    // These four methods MUST be implemented for Spring Security to function correctly.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Tie this to an 'active' boolean field if you add email verification later
    }

    // -------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null || roles.isEmpty()
                ? new HashSet<>(Set.of(Role.GUEST))
                : new HashSet<>(roles);
    }

    public void addRole(Role role) {
        if (role != null) {
            roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            roles.remove(role);
        }
    }

    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public void setPassword(String password) {
        this.password = validatePassword(password);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName != null && lastName.trim().length() > 50) {
            throw new IllegalArgumentException("lastName must not exceed 50 characters");
        }
        this.lastName = lastName != null ? lastName.trim() : null;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.trim().length() > 20) {
            throw new IllegalArgumentException("phoneNumber must not exceed 20 characters");
        }
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : null;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        if (bio != null && bio.trim().length() > 255) {
            throw new IllegalArgumentException("bio must not exceed 255 characters");
        }
        this.bio = bio != null ? bio.trim() : null;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // -------------------------------------------------------
    // Validation
    // -------------------------------------------------------

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String value = name.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("name must not exceed 100 characters");
        }
        return value;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        String value = email.trim().toLowerCase();
        if (value.length() > 150) {
            throw new IllegalArgumentException("email must not exceed 150 characters");
        }
        return value;
    }

    private static String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return password;
    }

    // -------------------------------------------------------
    // Equality
    // -------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                '}';
    }
}