package com.vinayakbhat.bookingSuite;

import com.vinayakbhat.bookingSuite.AdminHotel.Controller.HotelController;
import com.vinayakbhat.bookingSuite.AdminHotel.Service.HotelService;
import com.vinayakbhat.bookingSuite.AdminHotel.dto.HotelRequest;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Booking.Service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityRoleAccessTest {

    @Mock
    private HotelService hotelService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private HotelController hotelController;

    private User guestUser;
    private User hotelManagerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        guestUser = new User();
        guestUser.setId(1L);
        guestUser.setEmail("guest@example.com");
        guestUser.setName("Guest User");
        guestUser.setRoles(Set.of(Role.GUEST));

        hotelManagerUser = new User();
        hotelManagerUser.setId(2L);
        hotelManagerUser.setEmail("manager@example.com");
        hotelManagerUser.setName("Hotel Manager");
        hotelManagerUser.setRoles(Set.of(Role.HOTEL_MANAGER));

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setRoles(Set.of(Role.ADMIN));
    }

    @Test
    @DisplayName("HOTEL_MANAGER has correct ROLE_HOTEL_MANAGER authority")
    void hotelManager_hasCorrectAuthority() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(hotelManagerUser, null, hotelManagerUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(auth.getAuthorities())
                .extracting("authority")
                .contains("ROLE_HOTEL_MANAGER")
                .doesNotContain("ROLE_GUEST");
    }

    @Test
    @DisplayName("GUEST has only ROLE_GUEST and does not possess manager authorities")
    void guest_hasOnlyGuestAuthority() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(guestUser, null, guestUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(auth.getAuthorities())
                .extracting("authority")
                .contains("ROLE_GUEST")
                .doesNotContain("ROLE_HOTEL_MANAGER", "ROLE_ADMIN", "ROLE_OWNER");
    }

    @Test
    @DisplayName("HOTEL_MANAGER accessing getAllHotelByOwner returns 200 OK with hotel list")
    void hotelManagerAccessingAdminEndpoints_returnsSuccess() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(hotelManagerUser, null, hotelManagerUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(hotelService.getAllHotelOfOwner()).thenReturn(List.of(
                new HotelRequest(1L, "Grand Resort", "Goa", null, null, null, null)
        ));

        ResponseEntity<List<HotelRequest>> response = hotelController.getAllHotelByOwner();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("ADMIN accessing getAllHotelByOwner returns 200 OK")
    void adminAccessingAdminEndpoints_returnsSuccess() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(hotelService.getAllHotelOfOwner()).thenReturn(List.of());

        ResponseEntity<List<HotelRequest>> response = hotelController.getAllHotelByOwner();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
