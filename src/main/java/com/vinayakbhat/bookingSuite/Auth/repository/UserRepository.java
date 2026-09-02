package com.vinayakbhat.bookingSuite.Auth.repository;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String normalizedEmail);

    Optional<UserDetails> findByEmail(String username);
}


