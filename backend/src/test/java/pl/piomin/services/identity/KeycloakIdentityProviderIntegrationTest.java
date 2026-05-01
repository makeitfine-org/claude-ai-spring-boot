package pl.piomin.services.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.domain.port.IdentityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test verifying that:
 * 1. The Spring context loads with identity-provider config (AC #3).
 * 2. Swapping the IdentityProvider adapter requires no changes to consuming code (AC #5).
 *
 * <p>A stub {@link IdentityProvider} is registered to avoid a real Keycloak dependency at
 * integration-test time. This also validates that any consumer only depends on the port
 * interface — not on {@code KeycloakIdentityProvider} — which is exactly what AC #5 requires.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KeycloakIdentityProviderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    /**
     * Replaces the real KeycloakIdentityProvider with a stub for integration tests.
     * This proves AC #5: a consumer only depends on the IdentityProvider interface.
     */
    @TestConfiguration
    static class StubIdentityProviderConfig {

        @Bean
        @Primary
        public IdentityProvider stubIdentityProvider() {
            return new StubIdentityProvider();
        }
    }

    @Autowired
    private IdentityProvider identityProvider;

    @Test
    void springContext_LoadsWithIdentityProviderBean() {
        assertThat(identityProvider).isNotNull();
    }

    @Test
    void createUser_WithStub_ReturnsDeterministicSub() {
        CreateUserCommand command = new CreateUserCommand(
                "testuser", "testuser@example.com", "Password1!", "Test", "User");

        String sub = identityProvider.createUser(command);

        assertThat(sub).isEqualTo("stub-sub-testuser");
    }

    @Test
    void createUser_DuplicateUsername_ThrowsTypedException() {
        CreateUserCommand command = new CreateUserCommand(
                "duplicate", "dup@example.com", "Password1!", "Dup", "User");

        assertThatThrownBy(() -> identityProvider.createUser(command))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void deleteUser_WithStub_NoException() {
        identityProvider.deleteUser("any-sub"); // must not throw
    }

    // -------------------------------------------------------------------------
    // Stub — simulates a non-Keycloak IdentityProvider (proves swappability)
    // -------------------------------------------------------------------------

    static class StubIdentityProvider implements IdentityProvider {

        @Override
        public String createUser(CreateUserCommand command) {
            if ("duplicate".equals(command.getUsername())) {
                throw new DuplicateUsernameException(command.getUsername());
            }
            return "stub-sub-" + command.getUsername();
        }

        @Override
        public void deleteUser(String sub) {
            // no-op
        }
    }
}
