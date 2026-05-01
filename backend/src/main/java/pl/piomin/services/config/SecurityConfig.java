package pl.piomin.services.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pl.piomin.services.infrastructure.security.OidcLoginSuccessHandler;

import javax.crypto.SecretKey;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public SecurityConfig(UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Custom JwtDecoder using the application's local HMAC-SHA256 secret.
     * Overrides the auto-configured JWKS-based decoder so that tokens issued
     * by the local {@link pl.piomin.services.infrastructure.security.JwtService}
     * (used by the SPA login flow) are accepted by the OAuth2 resource server.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public OidcLoginSuccessHandler oidcLoginSuccessHandler() {
        return new OidcLoginSuccessHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        OidcClientInitiatedLogoutSuccessHandler oidcLogoutHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutHandler.setPostLogoutRedirectUri("{baseUrl}");

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // REST API endpoints do not use session cookies — CSRF protection is unnecessary
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**"))
                // Registration is a stateless REST endpoint — no session cookie involved
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"))
                // Use explicit AntPathRequestMatcher so CSRF is bypassed reliably in tests
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/logout"))
                // Profile API endpoints authenticated via JWT bearer — CSRF not needed
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.PATCH, "/api/users/me"))
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.DELETE, "/api/users/me"))
                // Avatar API endpoints authenticated via JWT bearer — CSRF not needed
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.PUT, "/api/users/me/avatar"))
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.DELETE, "/api/users/me/avatar"))
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.newSession())
            )
            .authorizeHttpRequests(auth -> auth
                // Use AntPathRequestMatcher to avoid MvcRequestMatcher issues in @WebMvcTest slices
                .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/refresh")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/oauth2/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/login/**")).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oidcLoginSuccessHandler())
            )
            .oauth2ResourceServer(rs -> rs
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .logout(logout -> logout
                .logoutRequestMatcher(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/logout"))
                .logoutSuccessHandler(oidcLogoutHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            // BFF: SPA handles redirect to /oauth2/authorization/keycloak on 401 itself
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
