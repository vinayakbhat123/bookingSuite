package com.vinayakbhat.bookingSuite.Auth.repository;


import com.vinayakbhat.bookingSuite.Auth.Entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findByEmailIgnoreCase(String email);
    @Modifying
    @Query("DELETE FROM EmailOtp e WHERE e.expiresAt < :now")
    void deleteByExpiresAtBefore(LocalDateTime now);
    void deleteByEmail(String email);
}