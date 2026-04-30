package pl.piomin.services.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Keycloak identity provider adapter.
 * All values are sourced from environment variables — no keycloak.* properties used.
 */
@Component
@ConfigurationProperties(prefix = "identity-provider")
public class KeycloakProperties {

    /** OIDC issuer URI, e.g. http://keycloak:8080/realms/app — sourced from OIDC_ISSUER_URI */
    private String issuerUri;

    /** Admin client ID used for service-account calls — sourced from KEYCLOAK_ADMIN_CLIENT_ID */
    private String adminClientId;

    /** Admin client secret — sourced from KEYCLOAK_ADMIN_CLIENT_SECRET */
    private String adminClientSecret;

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    /**
     * Derives the token endpoint URL from the issuer URI.
     * e.g. http://keycloak:8080/realms/app/protocol/openid-connect/token
     */
    public String getTokenEndpoint() {
        return issuerUri + "/protocol/openid-connect/token";
    }

    /**
     * Derives the Keycloak Admin REST API base URL for users from the issuer URI.
     * e.g. http://keycloak:8080/realms/app → http://keycloak:8080/admin/realms/app/users
     */
    public String getAdminUsersUrl() {
        // Replace /realms/ with /admin/realms/ to build the admin API URL
        return issuerUri.replace("/realms/", "/admin/realms/") + "/users";
    }
}
