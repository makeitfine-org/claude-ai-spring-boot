package pl.piomin.services.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.application.dto.RegistrationRequest;
import pl.piomin.services.application.dto.RegistrationResponse;
import pl.piomin.services.application.service.RegistrationService;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.PasswordPolicyViolationException;
import pl.piomin.services.infrastructure.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // POST /api/register -- happy path
    // -------------------------------------------------------------------------

    @Test
    void register_ValidRequest_Returns201WithBody() throws Exception {
        String sub = UUID.randomUUID().toString();
        RegistrationResponse response = new RegistrationResponse(sub, "john_doe", "john.doe@example.com");

        when(registrationService.register(any(RegistrationRequest.class))).thenReturn(response);

        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sub").value(sub))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(registrationService).register(any(RegistrationRequest.class));
    }

    // -------------------------------------------------------------------------
    // POST /api/register -- invalid username
    // -------------------------------------------------------------------------

    @Test
    void register_InvalidUsernamePattern_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john doe!", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void register_UsernameTooShort_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void register_UsernameTooLong_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "this_username_is_way_too_long", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/register -- invalid email
    // -------------------------------------------------------------------------

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "not-an-email", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/register -- blank fields
    // -------------------------------------------------------------------------

    @Test
    void register_BlankUsername_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void register_BlankPassword_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "john.doe@example.com", "", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void register_BlankDisplayName_Returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "john.doe@example.com", "Str0ng!Pass", "");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void register_DisplayNameWithControlCharacters_Returns400() throws Exception {
        // Build JSON manually to embed a tab character (0x09) as a control character in displayName.
        // The @NoControlCharacters constraint rejects strings containing chars <= 0x1F.
        String jsonBody = "{\"username\":\"john_doe\","
                + "\"email\":\"john.doe@example.com\","
                + "\"password\":\"Str0ng!Pass\","
                + "\"displayName\":\"John\\u0009Doe\"}";

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    // -------------------------------------------------------------------------
    // POST /api/register -- service throws DuplicateUsernameException -> 409
    // -------------------------------------------------------------------------

    @Test
    void register_DuplicateUsername_Returns409() throws Exception {
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new DuplicateUsernameException("john_doe"));

        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "john.doe@example.com", "Str0ng!Pass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // POST /api/register -- service throws PasswordPolicyViolationException -> 400
    // -------------------------------------------------------------------------

    @Test
    void register_PasswordPolicyViolation_Returns400() throws Exception {
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new PasswordPolicyViolationException(
                        List.of("password must be at least 8 characters long")));

        // Password is non-blank (passes @NotBlank) but service will throw PasswordPolicyViolationException
        RegistrationRequest request = new RegistrationRequest(
                "john_doe", "john.doe@example.com", "weakpass", "John Doe");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
