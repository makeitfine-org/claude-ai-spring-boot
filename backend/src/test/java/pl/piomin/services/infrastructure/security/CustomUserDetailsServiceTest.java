package pl.piomin.services.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final UUID TEST_SUB = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        customUserDetailsService = new CustomUserDetailsService(userRepository, passwordEncoder);
    }

    private User dbUser() {
        User user = new User();
        user.setSub(TEST_SUB);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        return user;
    }

    @Test
    void loadUserByUsername_KnownEmail_ReturnsUserDetailsWithSubAsUsername() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dbUser()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(TEST_SUB.toString());
        assertThat(passwordEncoder.matches("password", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_UuidIdentifier_LooksUpByPrimaryKey() {
        when(userRepository.findById(TEST_SUB)).thenReturn(Optional.of(dbUser()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(TEST_SUB.toString());

        assertThat(userDetails.getUsername()).isEqualTo(TEST_SUB.toString());
    }

    @Test
    void loadUserByUsername_UnknownEmail_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void loadUserByUsername_EmptyString_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
