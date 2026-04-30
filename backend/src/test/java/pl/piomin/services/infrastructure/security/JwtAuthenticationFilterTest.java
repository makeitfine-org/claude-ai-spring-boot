package pl.piomin.services.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // No Authorization header
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_NoAuthorizationHeader_ContinuesChainWithoutAuthentication() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // -------------------------------------------------------------------------
    // Non-Bearer prefix
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_NonBearerHeader_ContinuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // -------------------------------------------------------------------------
    // Valid Bearer token — valid JWT
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_ValidBearerToken_SetsAuthentication() throws Exception {
        UserDetails userDetails = new User("test@example.com", "pass", Collections.emptyList());
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userDetails);
    }

    // -------------------------------------------------------------------------
    // Valid Bearer token — invalid JWT (isTokenValid = false)
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_BearerToken_InvalidJwt_NoAuthenticationSet() throws Exception {
        UserDetails userDetails = new User("test@example.com", "pass", Collections.emptyList());
        request.addHeader("Authorization", "Bearer invalid.jwt.token");

        when(jwtService.extractUsername("invalid.jwt.token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid.jwt.token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // -------------------------------------------------------------------------
    // Exception during JWT parsing — filter continues chain gracefully
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_JwtServiceThrowsException_ContinuesChainGracefully() throws Exception {
        request.addHeader("Authorization", "Bearer malformed.token");

        when(jwtService.extractUsername("malformed.token"))
                .thenThrow(new RuntimeException("JWT parse error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // -------------------------------------------------------------------------
    // Authentication already set — should not re-authenticate
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_AuthenticationAlreadyPresent_SkipsLoadUserByUsername() throws Exception {
        UserDetails userDetails = new User("test@example.com", "pass", Collections.emptyList());
        request.addHeader("Authorization", "Bearer some.jwt.token");

        // Pre-set authentication in context
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername("some.jwt.token")).thenReturn("test@example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
