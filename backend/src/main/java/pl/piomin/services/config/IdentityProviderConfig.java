package pl.piomin.services.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the {@link RestClient} used by the identity provider adapter.
 * A dedicated bean keeps the IdP HTTP client isolated from other RestClient usages.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.iam", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdentityProviderConfig {

    /**
     * Creates a {@link RestClient} instance scoped to identity-provider calls.
     * The prototype-scoped {@link RestClient.Builder} provided by Spring Boot
     * is used so that {@code MockRestServiceServer} can bind to it in tests.
     *
     * @param builder auto-configured prototype builder injected by Spring Boot
     * @return a RestClient ready for Keycloak Admin REST API calls
     */
    @Bean
    public RestClient identityProviderRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
