package pl.piomin.services.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.piomin.services.config.KeycloakProperties;
import pl.piomin.services.domain.exception.DuplicateEmailException;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.IdentityProviderException;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.infrastructure.identity.KeycloakIdentityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KeycloakIdentityProviderTest {

    private static final String ISSUER_URI = "http://keycloak:8180/realms/app";
    private static final String TOKEN_ENDPOINT = ISSUER_URI + "/protocol/openid-connect/token";
    private static final String ADMIN_USERS_URL = "http://keycloak:8180/admin/realms/app/users";
    private static final String VALID_TOKEN_RESPONSE =
            "{\"access_token\":\"test-token\",\"token_type\":\"bearer\",\"expires_in\":300}";

    private MockRestServiceServer mockServer;
    private KeycloakIdentityProvider identityProvider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        KeycloakProperties properties = new KeycloakProperties();
        properties.setIssuerUri(ISSUER_URI);
        properties.setAdminClientId("admin-cli");
        properties.setAdminClientSecret("secret");

        identityProvider = new KeycloakIdentityProvider(builder.build(), properties);
    }

    // -------------------------------------------------------------------------
    // createUser — positive cases
    // -------------------------------------------------------------------------

    @Test
    void createUser_Success_ReturnsSubFromLocationHeader() {
        String expectedSub = "550e8400-e29b-41d4-a716-446655440000";

        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .location(java.net.URI.create(ADMIN_USERS_URL + "/" + expectedSub)));

        String sub = identityProvider.createUser(createCommand("alice", "alice@example.com"));

        assertThat(sub).isEqualTo(expectedSub);
        mockServer.verify();
    }

    // -------------------------------------------------------------------------
    // createUser — negative cases
    // -------------------------------------------------------------------------

    @Test
    void createUser_DuplicateUsername_ThrowsDuplicateUsernameException() {
        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .body("{\"errorMessage\":\"User exists with same username\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                identityProvider.createUser(createCommand("alice", "alice@example.com")))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("alice")
                .satisfies(e -> assertThat(((DuplicateUsernameException) e).getUsername()).isEqualTo("alice"));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsDuplicateEmailException() {
        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .body("{\"errorMessage\":\"User exists with same email\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                identityProvider.createUser(createCommand("alice2", "alice@example.com")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("alice@example.com")
                .satisfies(e -> assertThat(((DuplicateEmailException) e).getEmail()).isEqualTo("alice@example.com"));
    }

    @Test
    void createUser_TokenEndpointFails_ThrowsIdentityProviderException() {
        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"unauthorized_client\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                identityProvider.createUser(createCommand("alice", "alice@example.com")))
                .isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void createUser_NoLocationHeader_ThrowsIdentityProviderException() {
        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)); // no Location header

        assertThatThrownBy(() ->
                identityProvider.createUser(createCommand("alice", "alice@example.com")))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("Location header");
    }

    // -------------------------------------------------------------------------
    // deleteUser — positive cases
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_Success_NoException() {
        String sub = "550e8400-e29b-41d4-a716-446655440000";

        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL + "/" + sub))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        identityProvider.deleteUser(sub); // must not throw
        mockServer.verify();
    }

    // -------------------------------------------------------------------------
    // deleteUser — negative cases
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_NotFound_DoesNotThrow() {
        String sub = "non-existent-sub";

        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL + "/" + sub))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // should silently swallow NOT_FOUND
        identityProvider.deleteUser(sub);
        mockServer.verify();
    }

    @Test
    void deleteUser_ServerError_ThrowsIdentityProviderException() {
        String sub = "550e8400-e29b-41d4-a716-446655440000";

        mockServer.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(VALID_TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(ADMIN_USERS_URL + "/" + sub))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> identityProvider.deleteUser(sub))
                .isInstanceOf(IdentityProviderException.class);
    }

    // -------------------------------------------------------------------------
    // KeycloakProperties URL derivation
    // -------------------------------------------------------------------------

    @Test
    void keycloakProperties_TokenEndpoint_DerivedFromIssuerUri() {
        pl.piomin.services.config.KeycloakProperties props = new pl.piomin.services.config.KeycloakProperties();
        props.setIssuerUri("http://keycloak:8180/realms/app");
        props.setAdminClientId("admin-cli");
        props.setAdminClientSecret("secret");

        assertThat(props.getTokenEndpoint())
                .isEqualTo("http://keycloak:8180/realms/app/protocol/openid-connect/token");
        assertThat(props.getAdminUsersUrl())
                .isEqualTo("http://keycloak:8180/admin/realms/app/users");
        assertThat(props.getIssuerUri()).isEqualTo("http://keycloak:8180/realms/app");
        assertThat(props.getAdminClientId()).isEqualTo("admin-cli");
        assertThat(props.getAdminClientSecret()).isEqualTo("secret");
    }

    // -------------------------------------------------------------------------
    // CreateUserCommand accessors
    // -------------------------------------------------------------------------

    @Test
    void createUserCommand_Getters_ReturnConstructorValues() {
        CreateUserCommand cmd = new CreateUserCommand("bob", "bob@example.com", "pass", "Bob", "Jones");

        assertThat(cmd.getUsername()).isEqualTo("bob");
        assertThat(cmd.getEmail()).isEqualTo("bob@example.com");
        assertThat(cmd.getPassword()).isEqualTo("pass");
        assertThat(cmd.getFirstName()).isEqualTo("Bob");
        assertThat(cmd.getLastName()).isEqualTo("Jones");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private CreateUserCommand createCommand(String username, String email) {
        return new CreateUserCommand(username, email, "Password1!", "Alice", "Smith");
    }
}
