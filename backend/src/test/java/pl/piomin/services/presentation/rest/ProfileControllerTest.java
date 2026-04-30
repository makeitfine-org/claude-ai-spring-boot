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
import pl.piomin.services.application.dto.ProfileResponse;
import pl.piomin.services.application.dto.UpdateProfileRequest;
import pl.piomin.services.application.service.ProfileService;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.domain.exception.UserNotFoundException;
import pl.piomin.services.infrastructure.security.JwtService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class ProfileControllerTest {

    private static final String SUB = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // GET /api/users/me — unauthenticated -> 401
    // -------------------------------------------------------------------------

    @Test
    void getProfile_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/users/me — authenticated -> 200
    // -------------------------------------------------------------------------

    @Test
    void getProfile_WithValidJwt_Returns200WithProfileBody() throws Exception {
        ProfileResponse response = buildProfileResponse(UUID.fromString(SUB));
        when(profileService.getProfile(UUID.fromString(SUB))).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(SUB))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.hasAvatar").value(false));

        verify(profileService).getProfile(UUID.fromString(SUB));
    }

    // -------------------------------------------------------------------------
    // GET /api/users/me — user not found -> 404
    // -------------------------------------------------------------------------

    @Test
    void getProfile_UserNotFound_Returns404() throws Exception {
        UUID sub = UUID.fromString(SUB);
        when(profileService.getProfile(sub)).thenThrow(new UserNotFoundException(sub));

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/me — valid request -> 200
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_WithValidRequest_Returns200() throws Exception {
        ProfileResponse response = buildProfileResponse(UUID.fromString(SUB));
        response.setDisplayName("Updated Name");

        when(profileService.updateProfile(eq(UUID.fromString(SUB)), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name");

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"));

        verify(profileService).updateProfile(eq(UUID.fromString(SUB)), any(UpdateProfileRequest.class));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/me — displayName too long -> 400
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_DisplayNameTooLong_Returns400() throws Exception {
        String tooLong = "A".repeat(51);
        UpdateProfileRequest request = new UpdateProfileRequest(tooLong);

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/me — displayName with control characters -> 400
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_DisplayNameWithControlCharacters_Returns400() throws Exception {
        String jsonBody = "{\"displayName\":\"John\\u0009Doe\"}";

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/me — null displayName (skip update) -> 200
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_NullDisplayName_Returns200() throws Exception {
        ProfileResponse response = buildProfileResponse(UUID.fromString(SUB));
        when(profileService.updateProfile(eq(UUID.fromString(SUB)), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        String jsonBody = "{\"displayName\":null}";

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/me — authenticated -> 204
    // -------------------------------------------------------------------------

    @Test
    void deleteProfile_WithValidJwt_Returns204() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isNoContent());

        verify(profileService).deleteProfile(UUID.fromString(SUB));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/me — unauthenticated -> 401
    // -------------------------------------------------------------------------

    @Test
    void deleteProfile_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/me — unauthenticated -> 401
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_WithoutAuth_Returns401() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("New Name");

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProfileResponse buildProfileResponse(UUID sub) {
        return new ProfileResponse(
                sub,
                "john_doe",
                "John Doe",
                "john@example.com",
                false,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now()
        );
    }
}
