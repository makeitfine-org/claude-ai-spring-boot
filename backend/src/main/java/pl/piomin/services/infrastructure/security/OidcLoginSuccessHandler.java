package pl.piomin.services.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Handles successful OIDC logins by storing the {@code sub} claim from the ID token
 * into the HTTP session. Downstream services can retrieve the subject identifier
 * from the session attribute {@value #SESSION_ATTR_SUB} to look up local user profiles.
 * <p>Registered as a Spring bean via {@code SecurityConfig.oidcLoginSuccessHandler()}.
 */
public class OidcLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_ATTR_SUB = "oidc_sub";

    public OidcLoginSuccessHandler() {
        super("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String sub = oidcUser.getSubject();
            if (sub != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute(SESSION_ATTR_SUB, sub);
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
