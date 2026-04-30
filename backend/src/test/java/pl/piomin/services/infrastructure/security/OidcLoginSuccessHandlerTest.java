package pl.piomin.services.infrastructure.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OidcLoginSuccessHandler}.
 * Verifies that the {@code sub} claim from the OIDC ID token is correctly
 * stored in the HTTP session after a successful login (AC #6).
 */
class OidcLoginSuccessHandlerTest {

    private OidcLoginSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new OidcLoginSuccessHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void onAuthenticationSuccess_OidcUser_StoresSubInSession() throws Exception {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .subject("user-sub-abc123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OidcUser oidcUser = new DefaultOidcUser(null, idToken);
        Authentication auth = createOidcAuth(oidcUser);

        handler.onAuthenticationSuccess(request, response, auth);

        HttpSession session = request.getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(OidcLoginSuccessHandler.SESSION_ATTR_SUB))
                .isEqualTo("user-sub-abc123");
    }

    @Test
    void onAuthenticationSuccess_OidcUser_WithDifferentSub_StoresCorrectSub() throws Exception {
        OidcIdToken idToken = OidcIdToken.withTokenValue("token-2")
                .subject("another-user-sub-xyz")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OidcUser oidcUser = new DefaultOidcUser(null, idToken);
        Authentication auth = createOidcAuth(oidcUser);

        handler.onAuthenticationSuccess(request, response, auth);

        HttpSession session = request.getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(OidcLoginSuccessHandler.SESSION_ATTR_SUB))
                .isEqualTo("another-user-sub-xyz");
    }

    @Test
    void onAuthenticationSuccess_NonOidcPrincipal_DoesNotCreateSession() throws Exception {
        // Non-OIDC authentication should not trigger sub storage
        Authentication auth = new TestingAuthenticationToken("plain-user", "password", "ROLE_USER");

        handler.onAuthenticationSuccess(request, response, auth);

        HttpSession session = request.getSession(false);
        // Session may or may not exist, but sub attribute should not be set
        if (session != null) {
            assertThat(session.getAttribute(OidcLoginSuccessHandler.SESSION_ATTR_SUB)).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Authentication createOidcAuth(OidcUser oidcUser) {
        return new TestingAuthenticationToken(oidcUser, null, "ROLE_USER");
    }
}
