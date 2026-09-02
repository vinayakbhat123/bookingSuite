package com.vinayakbhat.bookingSuite;

import com.vinayakbhat.bookingSuite.AdminHotel.Entity.Hotel;
import com.vinayakbhat.bookingSuite.AdminHotel.Service.Impl.HotelServiceImpl;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelInfoResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelResponse;
import com.vinayakbhat.bookingSuite.AdminHotel.repository.HotelRepository;
import com.vinayakbhat.bookingSuite.AdminRoom.repository.RoomRepository;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Mapper.HotelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private User owner;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setEmail("owner@bookingsuite.com");
        owner.setName("Hotel Owner");
        owner.setRoles(Set.of(Role.HOTEL_MANAGER));

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setHotelName("Seaside Resort");
        hotel.setCityName("Goa");
        hotel.setOwner(owner);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("getAllHotelOfOwner returns list of hotels belonging to current manager")
    void getAllHotelOfOwner_returnsOwnerHotels() {
        when(hotelRepository.findByOwnerId(10L)).thenReturn(List.of(hotel));
        when(hotelMapper.toHotelRequest(hotel))
                .thenReturn(new HotelRequest(1L, "Seaside Resort", "Goa", null, null, null, null));

        List<HotelRequest> result = hotelService.getAllHotelOfOwner();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hotelName()).isEqualTo("Seaside Resort");
    }

    @Test
    @DisplayName("getHotelInfoById returns hotel info with rooms")
    void getHotelInfoById_returnsHotelInfo() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findByHotelId(1L)).thenReturn(List.of());

        HotelResponse hotelResponse = new HotelResponse(1L, "Seaside Resort", "Goa", null, null, null, true);
        when(hotelMapper.toHotelResponse(hotel)).thenReturn(hotelResponse);

        HotelInfoResponse response = hotelService.getHotelInfoById(1L);

        assertThat(response).isNotNull();
        assertThat(response.hotel().hotelName()).isEqualTo("Seaside Resort");
    }
}
