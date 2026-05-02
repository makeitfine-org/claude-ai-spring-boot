package pl.piomin.services.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.config.PasswordEncoderConfig;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.presentation.rest.PersonController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that when {@code app.iam.enabled=false} all endpoints are open (permitAll).
 */
@WebMvcTest(PersonController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = "app.iam.enabled=false")
class IamDisabledSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private pl.piomin.services.application.service.PersonService personService;

    @Test
    void protectedEndpoint_Anonymous_PermittedWhenIamDisabled() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "Expected IAM-disabled chain to permit /api/persons, got " + status;
                });
    }

    @Test
    void protectedEndpoint_Anonymous_NotUnauthorizedWhenIamDisabled() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Got 401 — IAM-disabled chain should not enforce authentication";
                });
    }
}
