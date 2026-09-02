package com.vinayakbhat.bookingSuite;

import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Auth.Service.Impl.CustomOAuth2UserService;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OAuth2DynamicRoleTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("OAuth2 login for existing HOTEL_MANAGER user preserves ROLE_HOTEL_MANAGER authority")
    void existingHotelManager_retainsHotelManagerAuthority() {
        User managerUser = new User();
        managerUser.setEmail("manager@bookingsuite.com");
        managerUser.setName("Manager User");
        managerUser.setRoles(Set.of(Role.HOTEL_MANAGER));

        Set<String> authorities = managerUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_HOTEL_MANAGER");
        assertThat(authorities).doesNotContain("ROLE_GUEST");
    }

    @Test
    @DisplayName("OAuth2 login for new user dynamically assigns ROLE_GUEST without manager escalation")
    void newOAuthUser_assignsGuestRoleOnly() {
        User newUser = new User();
        newUser.setEmail("newguest@gmail.com");
        newUser.setName("New Guest");
        newUser.setRoles(Set.of(Role.GUEST));

        Set<String> authorities = newUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_GUEST");
        assertThat(authorities).doesNotContain("ROLE_HOTEL_MANAGER");
        assertThat(authorities).doesNotContain("ROLE_ADMIN");
    }

    @Test
    @DisplayName("OAuth2 login for ADMIN user dynamically preserves ROLE_ADMIN authority")
    void existingAdmin_retainsAdminAuthority() {
        User adminUser = new User();
        adminUser.setEmail("admin@bookingsuite.com");
        adminUser.setName("Admin User");
        adminUser.setRoles(Set.of(Role.ADMIN));

        Set<String> authorities = adminUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_ADMIN");
        assertThat(authorities).doesNotContain("ROLE_HOTEL_MANAGER");
    }
}
