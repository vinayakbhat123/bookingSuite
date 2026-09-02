package com.vinayakbhat.bookingSuite;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;
import com.vinayakbhat.bookingSuite.Booking.Enum.BookingStatus;
import com.vinayakbhat.bookingSuite.Booking.Service.CheckoutService;
import com.vinayakbhat.bookingSuite.Booking.Service.Impl.BookingServiceImpl;
import com.vinayakbhat.bookingSuite.Booking.Strategy.Impl.PricingService;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingRequest;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.Booking.repository.BookingRepository;
import com.vinayakbhat.bookingSuite.Booking.repository.GuestsRepository;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.Mapper.BookingMapper;
import com.stripe.StripeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private GuestsRepository guestsRepository;

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private PricingService pricingService;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Hotel testHotel;
    private Room testRoom;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("guest@example.com");
        testUser.setName("John Doe");
        testUser.setRoles(Set.of(Role.GUEST));

        testHotel = new Hotel();
        testHotel.setId(10L);
        testHotel.setHotelName("Luxury Resort");
        testHotel.setCityName("Goa");

        testRoom = new Room();
        testRoom.setId(20L);
        testRoom.setHotel(testHotel);
        testRoom.setTotalCount(10);
        testRoom.setBasePrice(BigDecimal.valueOf(3000));

        testInventory = new Inventory();
        testInventory.setId(100L);
        testInventory.setHotel(testHotel);
        testInventory.setRoom(testRoom);
        testInventory.setDate(LocalDate.now().plusDays(1));
        testInventory.setTotalCount(10);
        testInventory.setBookedCount(0);
        testInventory.setReservedCount(0);
        testInventory.setPrice(BigDecimal.valueOf(3000));
        testInventory.setClosed(false);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Invalid stay dates (checkIn after checkOut) throws IllegalArgumentException")
    void initiateBooking_invalidDates_throwsException() {
        BookingRequest request = new BookingRequest(
                10L,
                20L,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(2),
                1
        );

        assertThatThrownBy(() -> bookingService.initiateBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly before check-out date");
    }

    @Test
    @DisplayName("Zero or negative room count throws IllegalArgumentException")
    void initiateBooking_invalidRoomsCount_throwsException() {
        BookingRequest request = new BookingRequest(
                10L,
                20L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                0
        );

        assertThatThrownBy(() -> bookingService.initiateBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("Successful booking initiation reserves inventory and returns BookingResponse")
    void initiateBooking_success_createsBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(10L, 20L, checkIn, checkOut, 1);

        when(hotelRepository.findById(10L)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(testRoom));
        when(inventoryRepository.findAndLockAvailableInventory(20L, checkIn, checkOut, 1))
                .thenReturn(List.of(testInventory));
        when(pricingService.calculateDynamicPricing(testInventory)).thenReturn(BigDecimal.valueOf(3000));

        Booking savedBooking = new Booking();
        savedBooking.setId(50L);
        savedBooking.setHotel(testHotel);
        savedBooking.setRoom(testRoom);
        savedBooking.setUser(testUser);
        savedBooking.setRoomsCount(1);
        savedBooking.setCheckInDate(checkIn);
        savedBooking.setCheckOutDate(checkOut);
        savedBooking.setStatus(BookingStatus.RESERVED);
        savedBooking.setPrice(BigDecimal.valueOf(3000));

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toBookingResponse(any(Booking.class)))
                .thenReturn(new BookingResponse(
                        50L,
                        BookingStatus.RESERVED,
                        1,
                        checkIn,
                        checkOut,
                        Set.of(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        BigDecimal.valueOf(3000),
                        10L,
                        "Luxury Resort",
                        "Goa",
                        20L,
                        "DELUXE",
                        "101",
                        "sess_123"
                ));

        BookingResponse response = bookingService.initiateBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo(BookingStatus.RESERVED);
    }
}
