package com.vinayakbhat.bookingSuite.Auth.Service.Impl;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Service.UserService;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileRequest;
import com.vinayakbhat.bookingSuite.Auth.dto.UserProfileResponse;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.repository.BookingRepository;
import com.vinayakbhat.bookingSuite.Mapper.BookingMapper;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final  UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper  bookingMapper;


    public UserServiceImpl(UserRepository userRepository, BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));}

    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public String updateProfile(UserProfileRequest request) {
        User user = getCurrentUser();
        updateFieldsIfPresent(user, request);
        return "Profile Updated Successfully";
    }

    @Override
    @Transactional(readOnly = true)
//    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfileResponse getUserProfile(Long userId) {
        User user = getCurrentUser();
        return userRepository.findById(user.getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User user = getCurrentUser();
        return bookingRepository.findByUser(user)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
    }
    private User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }

    private void updateFieldsIfPresent(User user, UserProfileRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        userRepository.save(user);
    }
    private UserProfileResponse mapToResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getBirthDate(),
                user.getGender(),
                user.getBio(),
                user.getRoles()
        );
    }
}
