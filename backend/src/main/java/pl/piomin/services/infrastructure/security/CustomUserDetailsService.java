package pl.piomin.services.infrastructure.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.piomin.services.domain.repository.UserRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String DEMO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<pl.piomin.services.domain.entity.User> dbUser = parseUuid(identifier)
                .flatMap(userRepository::findById)
                .or(() -> userRepository.findByEmail(identifier));

        return dbUser
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
    }

    private UserDetails toUserDetails(pl.piomin.services.domain.entity.User user) {
        // Self-registered users have a bcrypt hash of their chosen password.
        // The seeded test user has no hash — fall back to a hash of DEMO_PASSWORD
        // so legacy "password"-only logins keep working in dev/test.
        String passwordHash = user.getPasswordHash() != null
                ? user.getPasswordHash()
                : passwordEncoder.encode(DEMO_PASSWORD);
        return new User(
                user.getSub().toString(),
                passwordHash,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
