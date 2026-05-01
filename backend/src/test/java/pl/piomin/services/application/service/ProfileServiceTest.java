package pl.piomin.services.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.piomin.services.application.dto.ProfileResponse;
import pl.piomin.services.application.dto.UpdateProfileRequest;
import pl.piomin.services.domain.AuditEventType;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.UserNotFoundException;
import pl.piomin.services.domain.port.IdentityProvider;
import pl.piomin.services.domain.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentityProvider identityProvider;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProfileService profileService;

    // -------------------------------------------------------------------------
    // getProfile
    // -------------------------------------------------------------------------

    @Test
    void getProfile_UserFound_ReturnsProfileResponse() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "John Doe", "john@example.com", null);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        ProfileResponse response = profileService.getProfile(sub);

        assertThat(response.getSub()).isEqualTo(sub);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getDisplayName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.isHasAvatar()).isFalse();
    }

    @Test
    void getProfile_UserNotFound_ThrowsUserNotFoundException() {
        UUID sub = UUID.randomUUID();
        when(userRepository.findById(sub)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(sub))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(sub.toString());
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_WithDisplayName_UpdatesAndSaves() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "Old Name", "john@example.com", null);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest("  New Name  ");
        ProfileResponse response = profileService.updateProfile(sub, request);

        assertThat(user.getDisplayName()).isEqualTo("New Name");
        verify(userRepository).save(user);
        assertThat(response.getDisplayName()).isEqualTo("New Name");
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.DISPLAY_NAME_CHANGED),
                eq("Old Name"), eq("New Name"));
    }

    @Test
    void updateProfile_WithNullDisplayName_DoesNotChangeDisplayName() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "Existing Name", "john@example.com", null);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest(null);
        ProfileResponse response = profileService.updateProfile(sub, request);

        assertThat(user.getDisplayName()).isEqualTo("Existing Name");
        verify(userRepository).save(user);
        assertThat(response.getDisplayName()).isEqualTo("Existing Name");
    }

    @Test
    void updateProfile_UserNotFound_ThrowsUserNotFoundException() {
        UUID sub = UUID.randomUUID();
        when(userRepository.findById(sub)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest("New Name");

        assertThatThrownBy(() -> profileService.updateProfile(sub, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(sub.toString());

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteProfile
    // -------------------------------------------------------------------------

    @Test
    void deleteProfile_UserFound_DeletesAndCallsIdp() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "John Doe", "john@example.com", null);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        profileService.deleteProfile(sub);

        verify(userRepository).delete(user);
        verify(identityProvider).deleteUser(sub.toString());
        verify(auditService).log(eq(sub.toString()), eq(AuditEventType.ACCOUNT_DELETED));
        verify(auditService).anonymiseForDeletedUser(eq(sub.toString()));
    }

    @Test
    void deleteProfile_UserNotFound_ThrowsUserNotFoundException() {
        UUID sub = UUID.randomUUID();
        when(userRepository.findById(sub)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteProfile(sub))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(sub.toString());

        verify(userRepository, never()).delete(any(User.class));
        verify(identityProvider, never()).deleteUser(any());
    }

    // -------------------------------------------------------------------------
    // hasAvatar mapping
    // -------------------------------------------------------------------------

    @Test
    void getProfile_WithNonEmptyAvatarBytes_HasAvatarIsTrue() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "John Doe", "john@example.com", new byte[]{1, 2, 3});

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        ProfileResponse response = profileService.getProfile(sub);

        assertThat(response.isHasAvatar()).isTrue();
    }

    @Test
    void getProfile_WithEmptyAvatarBytes_HasAvatarIsFalse() {
        UUID sub = UUID.randomUUID();
        User user = buildUser(sub, "john_doe", "John Doe", "john@example.com", new byte[0]);

        when(userRepository.findById(sub)).thenReturn(Optional.of(user));

        ProfileResponse response = profileService.getProfile(sub);

        assertThat(response.isHasAvatar()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(UUID sub, String username, String displayName, String email, byte[] avatarBytes) {
        User user = new User();
        user.setSub(sub);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setAvatarBytes(avatarBytes);
        user.setCreatedAt(OffsetDateTime.now().minusDays(1));
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }
}
