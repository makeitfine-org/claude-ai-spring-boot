package pl.piomin.services.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the BFF security configuration.
 * Verifies public/protected path rules, OAuth2 login redirect, and logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Public paths (AC #5)
    // -------------------------------------------------------------------------

    @Test
    void healthEndpoint_NoAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpoint_NoAuthentication_IsPermitted() throws Exception {
        // POST /api/register is public — security allows it through.
        // The endpoint itself does not exist yet, so expect 404 (not 401/403).
        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\"}"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // OAuth2 login redirect (AC #1)
    // -------------------------------------------------------------------------

    @Test
    void oauth2Authorization_NoAuthentication_RedirectsToKeycloak() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("openid-connect/auth")));
    }

    // -------------------------------------------------------------------------
    // Protected paths — unauthenticated (AC #4)
    // -------------------------------------------------------------------------

    @Test
    void getPersons_NoAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPersonById_NoAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPerson_NoAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"t@t.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPersons_InvalidBearerToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Protected paths — authenticated via mock user (AC #4)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void getPersons_AuthenticatedUser_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Logout endpoint (AC #3)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void logout_AuthenticatedUser_RedirectsToIdp() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void logout_NoAuthentication_StillSucceeds() throws Exception {
        // Logout with no session is a no-op — Spring Security returns a redirect
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().is3xxRedirection());
    }

    // -------------------------------------------------------------------------
    // Session cookie — no token in response body (AC #2)
    // -------------------------------------------------------------------------

    @Test
    void oauth2AuthorizationCallback_DoesNotExposeTokensInBody() throws Exception {
        // The /oauth2/authorization/keycloak endpoint must redirect, not expose tokens
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().doesNotExist("Authorization"));
    }
}
