package pl.piomin.services.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pl.piomin.services.domain.AuditEventType;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.AvatarNotFoundException;
import pl.piomin.services.domain.exception.AvatarTooLargeException;
import pl.piomin.services.domain.exception.InvalidAvatarException;
import pl.piomin.services.domain.exception.UnsupportedAvatarTypeException;
import pl.piomin.services.domain.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AvatarService avatarService;

    // -------------------------------------------------------------------------
    // uploadAvatar — valid PNG
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_ValidPng_StoresBytes() throws IOException {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        byte[] pngBytes = createMinimalPng();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", pngBytes);

        avatarService.uploadAvatar(sub, file);

        verify(userRepository).save(user);
        assertThat(user.getAvatarBytes()).isNotNull();
        assertThat(user.getAvatarBytes().length).isGreaterThan(0);
        assertThat(user.getAvatarContentType()).isEqualTo("image/png");
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.AVATAR_UPLOADED));
    }

    // -------------------------------------------------------------------------
    // uploadAvatar — valid JPEG
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_ValidJpeg_StoresBytes() throws IOException {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        byte[] jpegBytes = createMinimalJpeg();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg",
                "image/jpeg", jpegBytes);

        avatarService.uploadAvatar(sub, file);

        verify(userRepository).save(user);
        assertThat(user.getAvatarBytes()).isNotNull();
        assertThat(user.getAvatarBytes().length).isGreaterThan(0);
        assertThat(user.getAvatarContentType()).isEqualTo("image/jpeg");
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.AVATAR_UPLOADED));
    }

    // -------------------------------------------------------------------------
    // uploadAvatar — wrong content type
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_WrongContentType_ThrowsUnsupportedAvatarTypeException() {
        UUID sub = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.gif",
                "image/gif", new byte[]{0x47, 0x49, 0x46});

        assertThatThrownBy(() -> avatarService.uploadAvatar(sub, file))
                .isInstanceOf(UnsupportedAvatarTypeException.class)
                .hasMessageContaining("image/gif");
    }

    // -------------------------------------------------------------------------
    // uploadAvatar — file too large
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_FileTooLarge_ThrowsAvatarTooLargeException() {
        UUID sub = UUID.randomUUID();
        byte[] largeBytes = new byte[1_048_577]; // 1 MB + 1 byte
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", largeBytes);

        assertThatThrownBy(() -> avatarService.uploadAvatar(sub, file))
                .isInstanceOf(AvatarTooLargeException.class)
                .hasMessageContaining("1048577");
    }

    // -------------------------------------------------------------------------
    // uploadAvatar — magic bytes mismatch
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_MagicBytesMismatch_ThrowsInvalidAvatarException() {
        UUID sub = UUID.randomUUID();
        // Declare as image/png but provide JPEG magic bytes
        byte[] fakeBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", fakeBytes);

        assertThatThrownBy(() -> avatarService.uploadAvatar(sub, file))
                .isInstanceOf(InvalidAvatarException.class)
                .hasMessageContaining("image/png");
    }

    // -------------------------------------------------------------------------
    // uploadAvatar — oversized image is downscaled
    // -------------------------------------------------------------------------

    @Test
    void uploadAvatar_OversizedImage_IsDownscaledToAtMost512() throws IOException {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        byte[] bigPngBytes = createPng(1024, 1024);
        MockMultipartFile file = new MockMultipartFile("file", "big.png",
                "image/png", bigPngBytes);

        avatarService.uploadAvatar(sub, file);

        // Verify stored image dimensions are ≤ 512
        byte[] stored = user.getAvatarBytes();
        assertThat(stored).isNotNull();
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(stored));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(512);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(512);
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.AVATAR_UPLOADED));
    }

    // -------------------------------------------------------------------------
    // getAvatarData — returns bytes when present
    // -------------------------------------------------------------------------

    @Test
    void getAvatarData_ReturnsBytes_WhenPresent() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        byte[] avatarBytes = {1, 2, 3, 4};
        user.setAvatarBytes(avatarBytes);
        user.setAvatarContentType("image/png");

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        AvatarService.AvatarData result = avatarService.getAvatarData(sub);

        assertThat(result.bytes()).isEqualTo(avatarBytes);
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    // -------------------------------------------------------------------------
    // getAvatarData — throws AvatarNotFoundException when null
    // -------------------------------------------------------------------------

    @Test
    void getAvatarData_ThrowsAvatarNotFoundException_WhenAvatarIsNull() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        user.setAvatarBytes(null);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> avatarService.getAvatarData(sub))
                .isInstanceOf(AvatarNotFoundException.class)
                .hasMessageContaining(sub.toString());
    }

    @Test
    void getAvatarData_ThrowsAvatarNotFoundException_WhenAvatarIsEmpty() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        user.setAvatarBytes(new byte[0]);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> avatarService.getAvatarData(sub))
                .isInstanceOf(AvatarNotFoundException.class)
                .hasMessageContaining(sub.toString());
    }

    // -------------------------------------------------------------------------
    // deleteAvatar — sets bytes and contentType to null
    // -------------------------------------------------------------------------

    @Test
    void deleteAvatar_SetsAvatarFieldsToNull() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub);
        user.setAvatarBytes(new byte[]{1, 2, 3});
        user.setAvatarContentType("image/png");

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        avatarService.deleteAvatar(sub);

        verify(userRepository).save(user);
        assertThat(user.getAvatarBytes()).isNull();
        assertThat(user.getAvatarContentType()).isNull();
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.AVATAR_REMOVED));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(UUID sub) {
        User user = new User();
        user.setSub(sub);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }

    private byte[] createMinimalPng() throws IOException {
        return createPng(1, 1);
    }

    private byte[] createPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private byte[] createMinimalJpeg() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }
}
