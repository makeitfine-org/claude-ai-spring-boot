package pl.piomin.services.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.piomin.services.application.dto.RegistrationRequest;
import pl.piomin.services.application.dto.RegistrationResponse;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.PasswordPolicyViolationException;
import pl.piomin.services.domain.exception.RegistrationException;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.domain.port.IdentityProvider;
import pl.piomin.services.domain.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private IdentityProvider identityProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private RegistrationService registrationService;

    private RegistrationRequest validRequest;
    private static final String VALID_SUB = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest(
                "john_doe",
                "john.doe@example.com",
                "Str0ng!Pass",
                "John Doe"
        );
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void register_ValidRequest_ReturnsRegistrationResponse() {
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(false);
        when(identityProvider.createUser(any(CreateUserCommand.class))).thenReturn(VALID_SUB);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(identityProvider).triggerEmailVerification(VALID_SUB);

        RegistrationResponse response = registrationService.register(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getSub()).isEqualTo(VALID_SUB);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");

        verify(passwordValidator).validate("Str0ng!Pass", "john_doe", "john.doe@example.com");
        verify(userRepository).existsByUsernameLowerCase("john_doe");
        verify(identityProvider).createUser(any(CreateUserCommand.class));
        verify(userRepository).saveAndFlush(any(User.class));
        verify(identityProvider).triggerEmailVerification(VALID_SUB);
    }

    @Test
    void register_ValidRequest_TrimsDisplayName() {
        validRequest.setDisplayName("  John Doe  ");
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(false);
        when(identityProvider.createUser(any(CreateUserCommand.class))).thenReturn(VALID_SUB);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertThat(saved.getDisplayName()).isEqualTo("John Doe");
            return saved;
        });

        registrationService.register(validRequest);

        verify(userRepository).saveAndFlush(any(User.class));
    }

    // -------------------------------------------------------------------------
    // Duplicate username
    // -------------------------------------------------------------------------

    @Test
    void register_DuplicateUsername_ThrowsDuplicateUsernameException() {
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("john_doe");

        verify(identityProvider, never()).createUser(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // Password policy violation
    // -------------------------------------------------------------------------

    @Test
    void register_PasswordPolicyViolation_ThrowsPasswordPolicyViolationException() {
        doThrow(new PasswordPolicyViolationException(List.of("password must be at least 8 characters long")))
                .when(passwordValidator).validate(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(PasswordPolicyViolationException.class);

        verify(userRepository, never()).existsByUsernameLowerCase(any());
        verify(identityProvider, never()).createUser(any());
    }

    // -------------------------------------------------------------------------
    // DB failure compensation
    // -------------------------------------------------------------------------

    @Test
    void register_DbInsertFailure_CompensatesByDeletingIdpUser() {
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(false);
        when(identityProvider.createUser(any(CreateUserCommand.class))).thenReturn(VALID_SUB);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("Registration failed after IdP user creation")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(identityProvider).deleteUser(VALID_SUB);
    }

    @Test
    void register_EmailVerificationFailure_CompensatesByDeletingIdpUser() {
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(false);
        when(identityProvider.createUser(any(CreateUserCommand.class))).thenReturn(VALID_SUB);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Email verification failed"))
                .when(identityProvider).triggerEmailVerification(VALID_SUB);

        assertThatThrownBy(() -> registrationService.register(validRequest))
                .isInstanceOf(RegistrationException.class);

        verify(identityProvider).deleteUser(VALID_SUB);
    }

    // -------------------------------------------------------------------------
    // IdP createUser is called with correct data
    // -------------------------------------------------------------------------

    @Test
    void register_CreatesIdpUserWithCorrectData() {
        when(userRepository.existsByUsernameLowerCase("john_doe")).thenReturn(false);
        when(identityProvider.createUser(any(CreateUserCommand.class))).thenReturn(VALID_SUB);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(validRequest);

        verify(identityProvider).createUser(argThat(cmd ->
                "john_doe".equals(cmd.getUsername()) &&
                "john.doe@example.com".equals(cmd.getEmail()) &&
                "Str0ng!Pass".equals(cmd.getPassword()) &&
                "John Doe".equals(cmd.getFirstName()) &&
                "John Doe".equals(cmd.getLastName())
        ));
    }
}
