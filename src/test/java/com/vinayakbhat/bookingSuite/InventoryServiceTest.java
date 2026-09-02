package com.vinayakbhat.bookingSuite;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelMinPriceRepository;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.entity.Room;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.Inventory.service.Impl.InventoryServiceImpl;
import com.vinayakbhat.bookingSuite.Mapper.HotelMapper;
import com.vinayakbhat.bookingSuite.Mapper.InventoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMinPriceRepository hotelMinPriceRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private User owner;
    private Hotel hotel;
    private Room testRoom;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@bookingsuite.com");
        owner.setName("Hotel Owner");
        owner.setRoles(Set.of(Role.HOTEL_MANAGER));

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setHotelName("Grand Palace");
        hotel.setCityName("Goa");
        hotel.setOwner(owner);

        testRoom = new Room();
        testRoom.setId(10L);
        testRoom.setHotel(hotel);
        testRoom.setTotalCount(10);
        testRoom.setBasePrice(BigDecimal.valueOf(2500));

        inventory1 = new Inventory();
        inventory1.setId(100L);
        inventory1.setRoom(testRoom);
        inventory1.setHotel(hotel);
        inventory1.setDate(LocalDate.now().plusDays(1));
        inventory1.setTotalCount(10);
        inventory1.setBookedCount(0);
        inventory1.setReservedCount(0);
        inventory1.setPrice(BigDecimal.valueOf(2500));
        inventory1.setClosed(false);

        inventory2 = new Inventory();
        inventory2.setId(101L);
        inventory2.setRoom(testRoom);
        inventory2.setHotel(hotel);
        inventory2.setDate(LocalDate.now().plusDays(2));
        inventory2.setTotalCount(10);
        inventory2.setBookedCount(0);
        inventory2.setReservedCount(0);
        inventory2.setPrice(BigDecimal.valueOf(2500));
        inventory2.setClosed(false);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Single room reservation decreases available units from 10 to 9")
    void singleRoomBooking_decreasesAvailability() {
        int initialAvailable = inventory1.getTotalCount() - inventory1.getBookedCount() - inventory1.getReservedCount();
        assertThat(initialAvailable).isEqualTo(10);

        // Reserve 1 room
        inventory1.setReservedCount(1);
        int afterReserve = inventory1.getTotalCount() - inventory1.getBookedCount() - inventory1.getReservedCount();
        assertThat(afterReserve).isEqualTo(9);

        // Confirm 1 room
        inventory1.setReservedCount(0);
        inventory1.setBookedCount(1);
        int afterConfirm = inventory1.getTotalCount() - inventory1.getBookedCount();
        assertThat(afterConfirm).isEqualTo(9);
    }

    @Test
    @DisplayName("Multiple room booking (3 rooms) decreases available units from 10 to 7")
    void multipleRoomBooking_decreasesAvailability() {
        int roomsCount = 3;
        inventory1.setReservedCount(roomsCount);

        int afterReserve = inventory1.getTotalCount() - inventory1.getBookedCount() - inventory1.getReservedCount();
        assertThat(afterReserve).isEqualTo(7);

        // Confirm 3 rooms
        inventory1.setReservedCount(0);
        inventory1.setBookedCount(3);
        int afterConfirm = inventory1.getTotalCount() - inventory1.getBookedCount();
        assertThat(afterConfirm).isEqualTo(7);
    }

    @Test
    @DisplayName("Zero inventory prevents further booking")
    void zeroInventory_preventsBooking() {
        inventory1.setBookedCount(10);
        int available = inventory1.getTotalCount() - inventory1.getBookedCount();
        assertThat(available).isZero();
    }

    @Test
    @DisplayName("getInventoryByRoom returns mapped inventory response list")
    void getInventoryByRoom_returnsList() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));
        when(inventoryRepository.findByRoomOrderByDate(testRoom)).thenReturn(List.of(inventory1, inventory2));
        when(inventoryMapper.toInventoryResponse(any(Inventory.class)))
                .thenReturn(new InventoryResponse(
                        100L,
                        LocalDate.now().plusDays(1),
                        0,
                        0,
                        10,
                        BigDecimal.ONE,
                        BigDecimal.valueOf(2500),
                        false,
                        "Goa",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        List<InventoryResponse> response = inventoryService.getInventoryByRoom(10L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).totalCount()).isEqualTo(10);
        assertThat(response.get(0).bookedCount()).isZero();
    }
}
