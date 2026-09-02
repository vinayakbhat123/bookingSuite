package com.vinayakbhat.bookingSuite.Auth.repository;

import com.vinayakbhat.bookingSuite.Auth.Entity.RefreshToken;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUser(User user);

    List<RefreshToken> findByUserOrderByIdAsc(User user);

}
