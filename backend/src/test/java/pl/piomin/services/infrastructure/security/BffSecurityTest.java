package pl.piomin.services.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.presentation.rest.PersonController;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-slice tests for the BFF {@link pl.piomin.services.config.SecurityConfig}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer and security configuration,
 * keeping tests fast without a real database or Keycloak instance.
 */
@WebMvcTest(PersonController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class BffSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private pl.piomin.services.application.service.PersonService personService;

    // -------------------------------------------------------------------------
    // Public paths (AC #5)
    // -------------------------------------------------------------------------

    @Test
    void registerEndpoint_Anonymous_IsPermitted() throws Exception {
        // Security permits POST /api/register even without session.
        // The endpoint does not exist yet → 404/405, NOT 401/403.
        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "Expected security to permit /api/register, but got " + status;
                });
    }

    // -------------------------------------------------------------------------
    // OAuth2 login redirect (AC #1)
    // -------------------------------------------------------------------------

    @Test
    void oauth2AuthorizationEndpoint_Anonymous_RedirectsToKeycloak() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("openid-connect/auth")));
    }

    // -------------------------------------------------------------------------
    // Protected paths — unauthenticated (AC #4)
    // -------------------------------------------------------------------------

    @Test
    void personEndpoint_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void personEndpoint_InvalidBearerToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Protected paths — authenticated via @WithMockUser
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void personEndpoint_AuthenticatedUser_Returns200() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // OIDC login — sub claim stored in session (AC #6)
    // -------------------------------------------------------------------------

    @Test
    void oidcLogin_OidcUserAuthenticated_Returns200() throws Exception {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .subject("test-user-sub-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", "test@example.com")
                .build();

        OidcUser oidcUser = new DefaultOidcUser(null, idToken);

        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(oidcLogin().oidcUser(oidcUser)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Logout (AC #3)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void logoutEndpoint_AuthenticatedUser_Redirects() throws Exception {
        mockMvc.perform(post("/api/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // -------------------------------------------------------------------------
    // Session cookie — no tokens exposed in response (AC #2)
    // -------------------------------------------------------------------------

    @Test
    void oauth2AuthorizationEndpoint_DoesNotExposeTokenInHeader() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().doesNotExist("Authorization"));
    }
}
