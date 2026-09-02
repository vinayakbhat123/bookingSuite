package com.vinayakbhat.bookingSuite.AdminHotel.repository;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByOwner(User owner);
    List<Hotel> findByOwnerId(Long ownerId);
}

