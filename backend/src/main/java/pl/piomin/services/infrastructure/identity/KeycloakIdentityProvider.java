package pl.piomin.services.infrastructure.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pl.piomin.services.domain.identity.CreateUserCommand;
import pl.piomin.services.domain.identity.DuplicateUsernameException;
import pl.piomin.services.domain.identity.IdentityProvider;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakIdentityProvider implements IdentityProvider {

    private final RestClient restClient;
    private final String issuerUri;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String adminBaseUrl;
    private final String realm;

    public KeycloakIdentityProvider(
            RestClient.Builder restClientBuilder,
            @Value("${oidc.issuer-uri:}") String issuerUri,
            @Value("${idp.admin-client-id:}") String adminClientId,
            @Value("${idp.admin-client-secret:}") String adminClientSecret) {
        this.restClient = restClientBuilder.build();
        this.issuerUri = issuerUri;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.adminBaseUrl = extractAdminBaseUrl(issuerUri);
        this.realm = extractRealm(issuerUri);
    }

    @Override
    public String createUser(CreateUserCommand command) {
        String adminToken = obtainAdminToken();

        Map<String, Object> userRepresentation = Map.of(
            "username", command.getUsername(),
            "email", command.getEmail(),
            "firstName", command.getFirstName(),
            "lastName", command.getLastName(),
            "enabled", true,
            "credentials", List.of(Map.of(
                "type", "password",
                "value", command.getPassword(),
                "temporary", false
            ))
        );

        var response = restClient.post()
            .uri(adminBaseUrl + "/admin/realms/" + realm + "/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(userRepresentation)
            .retrieve()
            .onStatus(status -> status.value() == 409, (req, res) -> {
                throw new DuplicateUsernameException(command.getUsername());
            })
            .toBodilessEntity();

        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("No Location header in Keycloak create user response");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public void deleteUser(String sub) {
        String adminToken = obtainAdminToken();

        restClient.delete()
            .uri(adminBaseUrl + "/admin/realms/" + realm + "/users/" + sub)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void triggerEmailVerification(String sub) {
        String adminToken = obtainAdminToken();

        restClient.put()
            .uri(adminBaseUrl + "/admin/realms/" + realm + "/users/" + sub + "/send-verify-email")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .retrieve()
            .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private String obtainAdminToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);

        Map<String, Object> tokenResponse = restClient.post()
            .uri(issuerUri + "/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(Map.class);

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new IllegalStateException("Failed to obtain admin token from Keycloak");
        }
        return (String) tokenResponse.get("access_token");
    }

    String extractAdminBaseUrl(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int realmsIndex = uri.indexOf("/realms/");
        return realmsIndex > 0 ? uri.substring(0, realmsIndex) : uri;
    }

    String extractRealm(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int realmsIndex = uri.indexOf("/realms/");
        if (realmsIndex < 0) {
            return "";
        }
        String afterRealms = uri.substring(realmsIndex + "/realms/".length());
        int nextSlash = afterRealms.indexOf('/');
        return nextSlash > 0 ? afterRealms.substring(0, nextSlash) : afterRealms;
    }
}
