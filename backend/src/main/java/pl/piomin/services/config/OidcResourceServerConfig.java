package pl.piomin.services.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration
public class OidcResourceServerConfig {

    @Bean
    @ConditionalOnExpression("!'${oidc.issuer-uri:}'.trim().isEmpty()")
    public JwtDecoder jwtDecoder(@Value("${oidc.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
