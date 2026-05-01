package pl.piomin.services.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.application.service.AvatarService;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.domain.exception.AvatarNotFoundException;
import pl.piomin.services.domain.exception.UnsupportedAvatarTypeException;
import pl.piomin.services.infrastructure.security.JwtService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvatarController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class AvatarControllerTest {

    private static final String SUB = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvatarService avatarService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // PUT /api/users/me/avatar — valid file returns 200
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_WithValidFile_Returns200() throws Exception {
        doNothing().when(avatarService).uploadAvatar(eq(UUID.fromString(SUB)), any());

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk());

        verify(avatarService).uploadAvatar(eq(UUID.fromString(SUB)), any());
    }

    // -------------------------------------------------------------------------
    // PUT /api/users/me/avatar — wrong content type returns 415
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_WrongContentType_Returns415() throws Exception {
        doThrow(new UnsupportedAvatarTypeException("image/gif"))
                .when(avatarService).uploadAvatar(eq(UUID.fromString(SUB)), any());

        MockMultipartFile file = new MockMultipartFile("file", "avatar.gif",
                "image/gif", new byte[]{0x47, 0x49, 0x46});

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isUnsupportedMediaType());
    }

    // -------------------------------------------------------------------------
    // GET /api/users/me/avatar — returns 200 with bytes and content type
    // -------------------------------------------------------------------------

    @Test
    void getAvatar_Returns200WithBytesAndContentType() throws Exception {
        byte[] avatarBytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00};
        AvatarService.AvatarData avatarData = new AvatarService.AvatarData(avatarBytes, "image/png");
        when(avatarService.getAvatarData(UUID.fromString(SUB))).thenReturn(avatarData);

        mockMvc.perform(get("/api/users/me/avatar")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(avatarBytes));
    }

    // -------------------------------------------------------------------------
    // GET /api/users/me/avatar — returns 404 when no avatar
    // -------------------------------------------------------------------------

    @Test
    void getAvatar_NoAvatar_Returns404() throws Exception {
        UUID sub = UUID.fromString(SUB);
        when(avatarService.getAvatarData(sub))
                .thenThrow(new AvatarNotFoundException(sub));

        mockMvc.perform(get("/api/users/me/avatar")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/me/avatar — returns 204
    // -------------------------------------------------------------------------

    @Test
    void deleteAvatar_Returns204() throws Exception {
        doNothing().when(avatarService).deleteAvatar(UUID.fromString(SUB));

        mockMvc.perform(delete("/api/users/me/avatar")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isNoContent());

        verify(avatarService).deleteAvatar(UUID.fromString(SUB));
    }

    // -------------------------------------------------------------------------
    // GET /api/users/me/avatar — unauthenticated returns 401
    // -------------------------------------------------------------------------

    @Test
    void getAvatar_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/me/avatar — unauthenticated returns 401
    // -------------------------------------------------------------------------

    @Test
    void deleteAvatar_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(delete("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());
    }
}
