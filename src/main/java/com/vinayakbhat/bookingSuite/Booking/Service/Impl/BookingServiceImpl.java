package com.vinayakbhat.bookingSuite.Booking.Service.Impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelReport;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;
import com.vinayakbhat.bookingSuite.Booking.Entity.Guest;
import com.vinayakbhat.bookingSuite.Booking.Enum.BookingStatus;
import com.vinayakbhat.bookingSuite.Booking.Service.CheckoutService;
import com.vinayakbhat.bookingSuite.Booking.Strategy.Impl.PricingService;
import com.vinayakbhat.bookingSuite.Booking.Strategy.PricingStrategy;
import com.vinayakbhat.bookingSuite.Booking.dto.GuestRequest;
import com.vinayakbhat.bookingSuite.Booking.repository.GuestsRepository;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import com.vinayakbhat.bookingSuite.Mapper.BookingMapper;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingRequest;
import com.vinayakbhat.bookingSuite.Booking.repository.BookingRepository;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Booking.Service.BookingService;
import com.vinayakbhat.bookingSuite.common.security.Exception.UnAuthorisedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final GuestsRepository guestsRepository;
    private final CheckoutService checkoutService;
    private final PricingService pricingService;
    private final StripeClient stripeClient;
    private final StringRedisTemplate stringRedisTemplate;


    public BookingServiceImpl(HotelRepository hotelRepository, RoomRepository roomRepository, InventoryRepository inventoryRepository, BookingRepository bookingRepository, BookingMapper bookingMapper,
                              GuestsRepository guestsRepository, CheckoutService checkoutService, PricingService pricingService, StripeClient stripeClient, StringRedisTemplate stringRedisTemplate) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.guestsRepository = guestsRepository;
        this.checkoutService = checkoutService;
        this.pricingService = pricingService;
        this.stripeClient = stripeClient;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public BookingResponse initiateBooking(BookingRequest bookingRequest) {
        log.info("Initiating Booking for hotel: {}, room: {}, roomsCount: {}",
                bookingRequest.hotelId(), bookingRequest.roomId(), bookingRequest.roomsCount());

        if (bookingRequest.roomsCount() == null || bookingRequest.roomsCount() <= 0) {
            throw new IllegalArgumentException("Requested rooms count must be greater than zero.");
        }
        if (bookingRequest.checkInDate() == null || bookingRequest.checkOutDate() == null || !bookingRequest.checkInDate().isBefore(bookingRequest.checkOutDate())) {
            throw new IllegalArgumentException("Check-in date must be strictly before check-out date.");
        }

        User user = getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException("Current User Not Found");
        }

        Hotel hotel = hotelRepository.findById(bookingRequest.hotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found: " + bookingRequest.hotelId()));
        Room room = roomRepository.findById(bookingRequest.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found: " + bookingRequest.roomId()));

        // Pessimistic write lock ensures atomic availability validation across all stay nights
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                room.getId(), bookingRequest.checkInDate(), bookingRequest.checkOutDate(), bookingRequest.roomsCount());
        long dateCount = ChronoUnit.DAYS.between(bookingRequest.checkInDate(), bookingRequest.checkOutDate());

        if (inventoryList.size() != dateCount) {
            throw new IllegalStateException("Requested room quantity is not available for the selected dates.");
        }

        // Reserve the room: update reservedCount directly on managed entities
        for (Inventory inv : inventoryList) {
            inv.setReservedCount(inv.getReservedCount() + bookingRequest.roomsCount());
        }

        BigDecimal priceForOneRoom = inventoryList.stream()
                .map(pricingService::calculateDynamicPricing)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDynamicPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.roomsCount()));

        inventoryRepository.saveAll(inventoryList);

        // create Booking
        Booking booking = new Booking();
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setUser(user);
        booking.setCheckInDate(bookingRequest.checkInDate());
        booking.setCheckOutDate(bookingRequest.checkOutDate());
        booking.setRoomsCount(bookingRequest.roomsCount());
        booking.setPrice(totalDynamicPrice);
        booking.setStatus(BookingStatus.RESERVED);
        booking = bookingRepository.save(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    // REDIS CACHE FIX: Instantly updates the Redis cache with the new BookingResponse
    // so the frontend immediately sees the added guests and new status on the next read.
    @CachePut(value = "booking", key = "#bookingId")
    public BookingResponse addGuests(Long bookingId, List<GuestRequest> guestRequests) {
        log.info("Adding Guests to Booking {}", bookingId);

        if (guestRequests == null || guestRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one guest must be provided.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found: " + bookingId));

        User user = getCurrentUser();
        if (user == null || booking.getUser() == null || !Objects.equals(user.getId(), booking.getUser().getId())) {
            throw new UnAuthorisedException("You are not authorized to modify booking with ID: " + bookingId);
        }

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking reservation has expired (10-minute timeout). Please initiate a new booking.");
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Cannot add guests: Booking status is " + booking.getStatus() + " (expected RESERVED).");
        }

        if (booking.getGuests() == null) {
            booking.setGuests(new java.util.HashSet<>());
        }

        for (GuestRequest guestRequest : guestRequests) {
            if (guestRequest == null || guestRequest.name() == null || guestRequest.name().isBlank()) {
                throw new IllegalArgumentException("Guest full name is required.");
            }
            Guest guest = new Guest(
                    user,
                    guestRequest.name().trim(),
                    guestRequest.gender() != null ? guestRequest.gender() : com.vinayakbhat.bookingSuite.Booking.Enum.Gender.OTHER,
                    guestRequest.age() != null && guestRequest.age() >= 0 && guestRequest.age() <= 150 ? guestRequest.age() : 18
            );
            if (guest.getBookings() == null) {
                guest.setBookings(new java.util.HashSet<>());
            }
            guest.getBookings().add(booking);
            guest = guestsRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    // REDIS CACHE FIX: Deletes the stale booking from Redis.
    // We cannot use @CachePut here because the method returns a String (URL), not a BookingResponse.
    @CacheEvict(value = "booking", key = "#bookingId")
    public String initiatePayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found: " + bookingId));
        User user = getCurrentUser();

        if (user == null || booking.getUser() == null || !Objects.equals(user.getId(), booking.getUser().getId())) {
            throw new UnAuthorisedException("You are not authorized to pay for booking with ID: " + bookingId);
        }

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking Expired");
        }

        // --- REDIS CHECKOUT LOCK REFRESH ---
        String lockKey = String.format("booking:lock:room:%d:dates:%s:%s",
                booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate());
        stringRedisTemplate.expire(lockKey, Duration.ofMinutes(10));
        // -----------------------------------

        String sessionUrl = checkoutService.getCheckoutSession(booking, frontendUrl + "/payment/success", frontendUrl + "/payment/failure");
        booking.setStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);
        return sessionUrl;
    }

    @Override
    @Transactional
    @CacheEvict(value = "booking", key = "#bookingId")
    public String canclePayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found: " + bookingId));
        User user = getCurrentUser();

        if (user == null || booking.getUser() == null || !Objects.equals(user.getId(), booking.getUser().getId())) {
            throw new UnAuthorisedException("You are not authorized to cancel booking with ID: " + bookingId);
        }

        // 3. Idempotency Check
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking Status Not in Confirmed cannot cancel Guests");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 4. Validate Inventory Lock
        List<Inventory> inventoryList = inventoryRepository.findAvailableInventoryForUpdate(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());

        if (inventoryList == null) {
            throw new IllegalStateException("Inventory shortage for booking " + booking.getId() + ". Payment succeeded but rooms are unavailable.");
        }
        inventoryRepository.cancelReservedBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());

        // Handle the refund
        try {
            if (booking.getStripeSessionId() != null && !booking.getStripeSessionId().isBlank()) {
                Session session = stripeClient.v1().checkout().sessions().retrieve(booking.getStripeSessionId());
                if (session.getPaymentIntent() != null) {
                    RefundCreateParams refundCreateParams = RefundCreateParams.builder()
                            .setPaymentIntent(session.getPaymentIntent())
                            .build();
                    stripeClient.v1().refunds().create(refundCreateParams);
                }
            }
        } catch (StripeException e) {
            log.error("Stripe refund error for booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
        return "Payment Successfully Refunded";
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found: " + bookingId));
        User user = getCurrentUser();
        if (user == null || booking.getUser() == null || !Objects.equals(user.getId(), booking.getUser().getId())) {
            throw new UnAuthorisedException("You are not authorized to view booking with ID: " + bookingId);
        }
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    @CacheEvict(value = "booking", key = "#bookingId")
    public BookingResponse verifyPayment(Long bookingId, String sessionId) {
        log.info("Verifying payment for booking ID: {}, sessionId: {}", bookingId, sessionId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found: " + bookingId));
        User user = getCurrentUser();
        if (user == null || booking.getUser() == null || !Objects.equals(user.getId(), booking.getUser().getId())) {
            throw new UnAuthorisedException("You are not authorized to verify booking with ID: " + bookingId);
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} is already CONFIRMED. Returning confirmed state.", bookingId);
            return bookingMapper.toBookingResponse(booking);
        }

        String effectiveSessionId = sessionId != null && !sessionId.isBlank()
                ? sessionId
                : booking.getStripeSessionId();

        if (effectiveSessionId == null || effectiveSessionId.isBlank()) {
            throw new IllegalStateException("No Stripe session ID available to verify booking: " + bookingId);
        }

        try {
            Session session = stripeClient.v1().checkout().sessions().retrieve(effectiveSessionId);
            String paymentStatus = session.getPaymentStatus();
            String sessionStatus = session.getStatus();
            log.info("Stripe Session {} paymentStatus={}, status={}", effectiveSessionId, paymentStatus, sessionStatus);

            if ("paid".equalsIgnoreCase(paymentStatus) || "complete".equalsIgnoreCase(sessionStatus)) {
                // Confirm inventory and mark booking as CONFIRMED
                List<Inventory> inventoryList = inventoryRepository.findAvailableInventoryForUpdate(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getRoomsCount()
                );

                long dateCount = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                if (inventoryList == null || inventoryList.size() != dateCount) {
                    throw new IllegalStateException("Inventory shortage for booking " + booking.getId() + ". Payment succeeded but rooms are unavailable.");
                }

                inventoryRepository.confirmReservedBooking(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getRoomsCount()
                );

                booking.setStatus(BookingStatus.CONFIRMED);
                booking = bookingRepository.save(booking);
                log.info("Booking {} successfully verified and updated to CONFIRMED.", bookingId);
            } else {
                log.warn("Stripe Session {} is not paid (paymentStatus={}, status={})", effectiveSessionId, paymentStatus, sessionStatus);
            }
        } catch (StripeException e) {
            log.error("Error retrieving Stripe session {}: {}", effectiveSessionId, e.getMessage());
            throw new IllegalStateException("Failed to verify Stripe payment: " + e.getMessage());
        }

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    // REDIS CACHE ADDITION: Caches the list of confirmed bookings for this hotel.
    @Cacheable(value = "hotel_bookings", key = "#hotelId")
    public List<BookingResponse> getAllBookingsByHotelId(Long hotelId){
        log.info("Getting all Bookings of Hotel {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found: " + hotelId));

        User user = getCurrentUser();

        if (user == null || hotel.getOwner() == null || !Objects.equals(user.getId(), hotel.getOwner().getId())) {
            throw new AccessDeniedException("You are not the Owner of this Hotel with id: " + hotelId);
        }

        List<Booking> bookingList = bookingRepository.findByHotelId(hotelId);
        return bookingList
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    // REDIS CACHE FIX: Caches the report using a combination of the Hotel ID and the Date Range
    @Cacheable(value = "hotelReports", key = "#hotelId + ':' + #startDate.toString() + ':' + #endDate.toString()")
    public HotelReport getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found: " + hotelId));

        User user = getCurrentUser();
        if (user == null || hotel.getOwner() == null || !Objects.equals(user.getId(), hotel.getOwner().getId())) {
            throw new AccessDeniedException("You are not the Owner of this Hotel with id: " + hotelId);
        }

        log.info("Getting Hotel Report for Hotel {}", hotelId);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookingsList = bookingRepository.findByHotelAndCreatedAtBetween(hotel,startDateTime,endDateTime);

        Long totalConfirmedBookings = bookingsList
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings = bookingsList.
                stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = totalConfirmedBookings > 0
                ? totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new HotelReport(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
    }

    private User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }

    private boolean hasBookingExpired(Booking booking) {
        if (booking == null || booking.getCreatedAt() == null) {
            return false;
        }
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
}
