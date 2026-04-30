package pl.piomin.services.infrastructure.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.piomin.services.domain.identity.CreateUserCommand;
import pl.piomin.services.domain.identity.DuplicateUsernameException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KeycloakIdentityProviderTest {

    private static final String ISSUER_URI = "http://keycloak:8180/realms/testrealm";
    private static final String ADMIN_CLIENT_ID = "admin-cli";
    private static final String ADMIN_SECRET = "admin-secret";
    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"test-admin-token\",\"token_type\":\"Bearer\"}";

    private KeycloakIdentityProvider provider;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new KeycloakIdentityProvider(builder, ISSUER_URI, ADMIN_CLIENT_ID, ADMIN_SECRET);
    }

    // --- createUser ---

    @Test
    void createUser_Success_ReturnsSubFromLocationHeader() {
        mockServer.expect(requestTo(ISSUER_URI + "/protocol/openid-connect/token"))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://keycloak:8180/admin/realms/testrealm/users"))
            .andRespond(withStatus(HttpStatus.CREATED)
                .location(URI.create("http://keycloak:8180/admin/realms/testrealm/users/user-abc-123")));

        String sub = provider.createUser(command("john", "john@example.com", "pass", "John", "Doe"));

        assertThat(sub).isEqualTo("user-abc-123");
        mockServer.verify();
    }

    @Test
    void createUser_DuplicateUsername_ThrowsDuplicateUsernameException() {
        mockServer.expect(requestTo(ISSUER_URI + "/protocol/openid-connect/token"))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://keycloak:8180/admin/realms/testrealm/users"))
            .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> provider.createUser(command("john", "john@example.com", "pass", "John", "Doe")))
            .isInstanceOf(DuplicateUsernameException.class)
            .hasMessageContaining("john");
    }

    @Test
    void createUser_NoLocationHeader_ThrowsIllegalStateException() {
        mockServer.expect(requestTo(ISSUER_URI + "/protocol/openid-connect/token"))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://keycloak:8180/admin/realms/testrealm/users"))
            .andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> provider.createUser(command("john", "john@example.com", "pass", "John", "Doe")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Location header");
    }

    // --- deleteUser ---

    @Test
    void deleteUser_Success() {
        mockServer.expect(requestTo(ISSUER_URI + "/protocol/openid-connect/token"))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://keycloak:8180/admin/realms/testrealm/users/user-abc-123"))
            .andRespond(withNoContent());

        provider.deleteUser("user-abc-123");

        mockServer.verify();
    }

    // --- triggerEmailVerification ---

    @Test
    void triggerEmailVerification_Success() {
        mockServer.expect(requestTo(ISSUER_URI + "/protocol/openid-connect/token"))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://keycloak:8180/admin/realms/testrealm/users/user-abc-123/send-verify-email"))
            .andRespond(withNoContent());

        provider.triggerEmailVerification("user-abc-123");

        mockServer.verify();
    }

    // --- URL parsing helpers ---

    @Test
    void extractAdminBaseUrl_StandardKeycloakUri() {
        assertThat(provider.extractAdminBaseUrl("http://keycloak:8180/realms/myrealm"))
            .isEqualTo("http://keycloak:8180");
    }

    @Test
    void extractAdminBaseUrl_EmptyUri_ReturnsEmpty() {
        assertThat(provider.extractAdminBaseUrl("")).isEqualTo("");
    }

    @Test
    void extractAdminBaseUrl_NoRealmsSegment_ReturnsFullUri() {
        assertThat(provider.extractAdminBaseUrl("http://keycloak:8180"))
            .isEqualTo("http://keycloak:8180");
    }

    @Test
    void extractRealm_StandardKeycloakUri() {
        assertThat(provider.extractRealm("http://keycloak:8180/realms/myrealm"))
            .isEqualTo("myrealm");
    }

    @Test
    void extractRealm_EmptyUri_ReturnsEmpty() {
        assertThat(provider.extractRealm("")).isEqualTo("");
    }

    @Test
    void extractRealm_NoRealmsSegment_ReturnsEmpty() {
        assertThat(provider.extractRealm("http://keycloak:8180")).isEqualTo("");
    }

    @Test
    void extractRealm_UriWithTrailingPath() {
        assertThat(provider.extractRealm("http://keycloak:8180/realms/myrealm/extra"))
            .isEqualTo("myrealm");
    }

    private CreateUserCommand command(String username, String email, String password,
                                      String firstName, String lastName) {
        return new CreateUserCommand(username, email, password, firstName, lastName);
    }
}
