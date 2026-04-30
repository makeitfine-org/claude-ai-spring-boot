package pl.piomin.services.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        customUserDetailsService = new CustomUserDetailsService(passwordEncoder);
    }

    // -------------------------------------------------------------------------
    // Happy path — known user
    // -------------------------------------------------------------------------

    @Test
    void loadUserByUsername_KnownUser_ReturnsUserDetails() {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isNotBlank();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_KnownUser_PasswordIsEncoded() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // The encoded password must match the raw "password" used in the service
        assertThat(passwordEncoder.matches("password", userDetails.getPassword())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Negative case — unknown user
    // -------------------------------------------------------------------------

    @Test
    void loadUserByUsername_UnknownUser_ThrowsUsernameNotFoundException() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void loadUserByUsername_EmptyString_ThrowsUsernameNotFoundException() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
