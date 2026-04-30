package pl.piomin.services.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.application.dto.AuthRequest;
import pl.piomin.services.application.dto.AuthResponse;
import pl.piomin.services.application.service.AuthenticationService;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    /**
     * Override the SecurityConfig-defined JwtDecoder so that the
     * BearerTokenAuthenticationFilter can be controlled in tests.
     * Without this, any request carrying "Authorization: Bearer ..." would
     * be rejected by the resource-server filter before reaching the controller.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // -------------------------------------------------------------------------
    // POST /api/auth/login — happy path
    // -------------------------------------------------------------------------

    @Test
    void login_ValidRequest_Returns200WithTokens() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authenticationService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authenticationService).login(any(AuthRequest.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — validation failure (blank email)
    // -------------------------------------------------------------------------

    @Test
    void login_BlankEmail_Returns400() throws Exception {
        AuthRequest request = new AuthRequest("", "password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — validation failure (invalid email format)
    // -------------------------------------------------------------------------

    @Test
    void login_InvalidEmailFormat_Returns400() throws Exception {
        AuthRequest request = new AuthRequest("not-an-email", "password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — validation failure (blank password)
    // -------------------------------------------------------------------------

    @Test
    void login_BlankPassword_Returns400() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/refresh — happy path with valid Bearer header
    //
    // The BearerTokenAuthenticationFilter intercepts any "Authorization: Bearer ..."
    // request. We stub jwtDecoder to return a valid Jwt so the filter passes through,
    // then the controller method receives the raw header string and calls the service.
    // -------------------------------------------------------------------------

    @Test
    void refresh_ValidBearerHeader_Returns200WithNewTokens() throws Exception {
        AuthResponse response = new AuthResponse("new-access-token", "existing-refresh-token");

        // Allow the resource-server filter to accept the bearer token
        Jwt stubJwt = Jwt.withTokenValue("existing-refresh-token")
                .header("alg", "HS256")
                .claim("sub", "test@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("existing-refresh-token")).thenReturn(stubJwt);

        when(authenticationService.refreshToken(eq("existing-refresh-token"))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer existing-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("existing-refresh-token"));

        verify(authenticationService).refreshToken("existing-refresh-token");
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/refresh — missing Authorization header
    //
    // MissingRequestHeaderException is not mapped to 400 by GlobalExceptionHandler;
    // it falls through to the generic Exception handler which returns 500.
    // -------------------------------------------------------------------------

    @Test
    void refresh_MissingAuthorizationHeader_Returns500() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, never()).refreshToken(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/refresh — non-Bearer prefix -> 400
    // The controller itself returns 400 when the header doesn't start with "Bearer "
    // -------------------------------------------------------------------------

    @Test
    void refresh_NonBearerPrefix_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).refreshToken(any());
    }
}
