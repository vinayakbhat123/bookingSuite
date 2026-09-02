package com.vinayakbhat.bookingSuite.Booking.repository;

import com.vinayakbhat.bookingSuite.Booking.Entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestsRepository extends JpaRepository<Guest,Long> {
}
