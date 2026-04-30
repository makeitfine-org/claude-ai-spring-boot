package pl.piomin.services.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import pl.piomin.services.application.dto.AuthRequest;
import pl.piomin.services.application.dto.AuthResponse;
import pl.piomin.services.infrastructure.security.JwtService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private UserDetails buildUser(String email) {
        return new User(email, "encodedPassword", Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    void login_ValidCredentials_ReturnsAuthResponse() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        UserDetails userDetails = buildUser("test@example.com");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        AuthResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("test@example.com");
        verify(jwtService).generateToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
    }

    @Test
    void login_InvalidCredentials_ThrowsBadCredentialsException() {
        AuthRequest request = new AuthRequest("test@example.com", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    // -------------------------------------------------------------------------
    // refreshToken
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_ValidToken_ReturnsNewAccessToken() {
        String existingRefreshToken = "valid-refresh-token";
        UserDetails userDetails = buildUser("test@example.com");

        when(jwtService.extractUsername(existingRefreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(existingRefreshToken, userDetails)).thenReturn(true);
        when(jwtService.generateToken(userDetails)).thenReturn("new-access-token");

        AuthResponse response = authenticationService.refreshToken(existingRefreshToken);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(existingRefreshToken);
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(jwtService).extractUsername(existingRefreshToken);
        verify(userDetailsService).loadUserByUsername("test@example.com");
        verify(jwtService).isTokenValid(existingRefreshToken, userDetails);
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void refreshToken_InvalidToken_ThrowsIllegalArgumentException() {
        String badRefreshToken = "expired-refresh-token";
        UserDetails userDetails = buildUser("test@example.com");

        when(jwtService.extractUsername(badRefreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(badRefreshToken, userDetails)).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refreshToken(badRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");

        verify(jwtService, never()).generateToken(any());
    }
}
