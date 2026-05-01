package pl.piomin.services.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.application.dto.RegistrationRequest;
import pl.piomin.services.application.dto.RegistrationResponse;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.RegistrationException;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.domain.port.IdentityProvider;
import pl.piomin.services.domain.repository.UserRepository;

import java.util.UUID;

@Service
@Transactional
public class RegistrationService {

    private final IdentityProvider identityProvider;
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;

    public RegistrationService(IdentityProvider identityProvider,
                               UserRepository userRepository,
                               PasswordValidator passwordValidator) {
        this.identityProvider = identityProvider;
        this.userRepository = userRepository;
        this.passwordValidator = passwordValidator;
    }

    /**
     * Registers a new user by:
     * <ol>
     *   <li>Validating the password policy</li>
     *   <li>Checking for a duplicate username (case-insensitive)</li>
     *   <li>Creating the user in the identity provider (already email-verified)</li>
     *   <li>Persisting a local users row</li>
     * </ol>
     * If the local persistence step fails, the IdP user is deleted as compensation
     * before rethrowing.
     *
     * @param request the registration request
     * @return the registration response containing sub, username, and email
     */
    public RegistrationResponse register(RegistrationRequest request) {
        passwordValidator.validate(request.getPassword(), request.getUsername(), request.getEmail());

        if (userRepository.existsByUsernameLowerCase(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        // Keycloak's default user profile requires both firstName and lastName.
        // The SPA only collects a single displayName, so mirror it into both
        // attributes — otherwise Keycloak triggers UPDATE_PROFILE on first login.
        CreateUserCommand command = new CreateUserCommand(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getDisplayName()
        );
        String sub = identityProvider.createUser(command);

        try {
            User user = new User();
            user.setSub(UUID.fromString(sub));
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setDisplayName(request.getDisplayName().trim());
            userRepository.saveAndFlush(user);
        } catch (Exception e) {
            identityProvider.deleteUser(sub);
            throw new RegistrationException("Registration failed after IdP user creation", e);
        }

        return new RegistrationResponse(sub, request.getUsername(), request.getEmail());
    }
}
