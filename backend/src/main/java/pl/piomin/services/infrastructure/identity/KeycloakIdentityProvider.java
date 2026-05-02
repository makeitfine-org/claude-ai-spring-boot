package pl.piomin.services.infrastructure.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.piomin.services.config.KeycloakProperties;
import pl.piomin.services.domain.exception.DuplicateEmailException;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.IdentityProviderException;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.domain.port.IdentityProvider;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Keycloak-specific adapter implementing the {@link IdentityProvider} port.
 * All Keycloak API calls are confined to this class.
 * Uses the Keycloak Admin REST API via plain {@link RestClient}.
 */
@Component
@ConditionalOnProperty(prefix = "app.iam", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakIdentityProvider implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProvider.class);

    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String DUPLICATE_EMAIL_MSG = "User exists with same email";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakIdentityProvider(@Qualifier("identityProviderRestClient") RestClient restClient,
                                    KeycloakProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String createUser(CreateUserCommand command) {
        String token = obtainAdminToken();

        Map<String, Object> userRepresentation = buildUserRepresentation(command);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(properties.getAdminUsersUrl())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userRepresentation)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new IdentityProviderException("Keycloak did not return a Location header after user creation");
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                String body = ex.getResponseBodyAsString();
                if (body.contains(DUPLICATE_EMAIL_MSG)) {
                    throw new DuplicateEmailException(command.getEmail());
                }
                throw new DuplicateUsernameException(command.getUsername());
            }
            throw new IdentityProviderException(
                    "Failed to create user in Keycloak: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteUser(String sub) {
        String token = obtainAdminToken();

        try {
            restClient.delete()
                    .uri(properties.getAdminUsersUrl() + "/{id}", sub)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User with sub={} not found in Keycloak during delete — skipping", sub);
                return;
            }
            throw new IdentityProviderException(
                    "Failed to delete user " + sub + " from Keycloak: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String obtainAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        form.add("client_id", properties.getAdminClientId());
        form.add("client_secret", properties.getAdminClientSecret());

        try {
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(properties.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new IdentityProviderException("Keycloak token response did not contain access_token");
            }
            return (String) tokenResponse.get("access_token");
        } catch (HttpClientErrorException ex) {
            throw new IdentityProviderException(
                    "Failed to obtain admin token from Keycloak: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildUserRepresentation(CreateUserCommand command) {
        return Map.of(
                "username", command.getUsername(),
                "email", command.getEmail(),
                "firstName", command.getFirstName() != null ? command.getFirstName() : "",
                "lastName", command.getLastName() != null ? command.getLastName() : "",
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", command.getPassword(),
                        "temporary", false
                ))
        );
    }
}
