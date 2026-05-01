package pl.piomin.services.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import pl.piomin.services.application.dto.RegistrationRequest;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.repository.AuditEventRepository;
import pl.piomin.services.domain.repository.UserRepository;
import pl.piomin.services.infrastructure.identity.KeycloakIdentityProvider;

import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests covering all user registration and profile scenarios.
 *
 * <p>Testcontainers spins up real Keycloak (with realm import) and Postgres instances.
 * The {@link KeycloakIdentityProvider#triggerEmailVerification} step is stubbed via
 * {@code @MockitoSpyBean} because the test environment has no SMTP server — all other
 * IdP interactions use the real Keycloak container.
 *
 * <p>Acceptance criteria covered:
 * <ul>
 *   <li>AC#1  — Keycloak + Postgres containers start automatically</li>
 *   <li>AC#2  — All 10 test scenarios pass</li>
 *   <li>AC#3  — Compensating-delete: Keycloak user removed when DB save fails</li>
 *   <li>AC#4  — Rate-limit: 6th registration attempt from same IP returns 429 + Retry-After</li>
 *   <li>AC#5  — Audit anonymisation: deleted-user rows start with {@code DELETED:}</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserRegistrationIntegrationTest {

    // -------------------------------------------------------------------------
    // Containers — started once for the whole test class (static)
    // -------------------------------------------------------------------------

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("keycloak/test-realm.json"),
                    "/opt/keycloak/data/import/test-realm.json")
            .withCommand("start-dev", "--import-realm")
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HTTP_PORT", "8080")
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
            .withExposedPorts(8080)
            .waitingFor(
                    Wait.forHttp("/realms/test/.well-known/openid-configuration")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    // -------------------------------------------------------------------------
    // Dynamic properties — set before the Spring context loads
    // -------------------------------------------------------------------------

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        String keycloakBase = "http://localhost:" + keycloak.getMappedPort(8080);
        String realmBase = keycloakBase + "/realms/test";

        registry.add("identity-provider.issuer-uri", () -> realmBase);
        registry.add("identity-provider.admin-client-id", () -> "backend-admin");
        registry.add("identity-provider.admin-client-secret", () -> "backend-admin-secret");

        // Keep OAuth2 client provider pointing to the test Keycloak so that
        // the Spring Security OIDC redirect URLs are formed correctly.
        registry.add("spring.security.oauth2.client.provider.keycloak.authorization-uri",
                () -> realmBase + "/protocol/openid-connect/auth");
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> realmBase + "/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.client.provider.keycloak.user-info-uri",
                () -> realmBase + "/protocol/openid-connect/userinfo");
        registry.add("spring.security.oauth2.client.provider.keycloak.jwk-set-uri",
                () -> realmBase + "/protocol/openid-connect/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> realmBase + "/protocol/openid-connect/certs");
    }

    // -------------------------------------------------------------------------
    // Spring beans
    // -------------------------------------------------------------------------

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Spy on the real KeycloakIdentityProvider so we can no-op
     * {@code triggerEmailVerification} (no SMTP in test environment)
     * while keeping real {@code createUser} / {@code deleteUser} behaviour.
     */
    @MockitoSpyBean
    private KeycloakIdentityProvider keycloakIdentityProvider;

    // -------------------------------------------------------------------------
    // Test lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // No-op email verification: avoids SMTP errors without losing real IdP calls.
        doNothing().when(keycloakIdentityProvider).triggerEmailVerification(any());

        // Clean DB state — audit_events first (sub is not a FK, but clean order is nice)
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // Scenario 1 — POST /api/register valid → 201; Keycloak user + DB row created
    // =========================================================================

    @Test
    void register_ValidRequest_Returns201_UserExistsInKeycloakAndDb() throws Exception {
        RegistrationRequest req = new RegistrationRequest(
                "usrscn01", "usrscn01@example.com", "Str0ng!Pass", "Test User One");

        String body = mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("usrscn01"))
                .andExpect(jsonPath("$.email").value("usrscn01@example.com"))
                .andExpect(jsonPath("$.sub").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String sub = objectMapper.readTree(body).get("sub").asText();

        // DB row must exist
        assertThat(userRepository.findById(UUID.fromString(sub))).isPresent();

        // Keycloak user must exist
        assertThat(keycloakUserExistsByUsername("usrscn01")).isTrue();
    }

    // =========================================================================
    // Scenario 2 — POST /api/register duplicate username → 409
    // =========================================================================

    @Test
    void register_DuplicateUsername_Returns409() throws Exception {
        RegistrationRequest first = new RegistrationRequest(
                "usrscn02", "usrscn02a@example.com", "Str0ng!Pass", "First");

        mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Second attempt with same username, different email
        RegistrationRequest duplicate = new RegistrationRequest(
                "usrscn02", "usrscn02b@example.com", "Str0ng!Pass", "Second");

        mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // Scenario 3 — POST /api/register weak password → 400
    // =========================================================================

    @Test
    void register_WeakPassword_Returns400() throws Exception {
        RegistrationRequest req = new RegistrationRequest(
                "usrscn03", "usrscn03@example.com", "weakpass", "Test User Three");

        mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Scenario 4 / AC#4 — 6 registrations from same IP → 6th returns 429 + Retry-After
    // =========================================================================

    @Test
    void register_SixAttemptsFromSameIp_SixthReturns429WithRetryAfter() throws Exception {
        // Isolate rate-limit state by using a dedicated IP that no other test uses.
        String testIp = "10.99.99.99";

        for (int i = 1; i <= 5; i++) {
            RegistrationRequest req = new RegistrationRequest(
                    "rateuse0" + i,
                    "rateuse0" + i + "@example.com",
                    "Str0ng!Pass",
                    "Rate User " + i);

            mockMvc.perform(post("/api/register")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 6th attempt — rate limiter blocks before the controller is reached
        RegistrationRequest sixth = new RegistrationRequest(
                "rateuse06", "rateuse06@example.com", "Str0ng!Pass", "Rate User Six");

        mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixth)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    // =========================================================================
    // Scenario 5 — Authenticated GET /api/users/me → 200
    // =========================================================================

    @Test
    void getProfile_Authenticated_Returns200() throws Exception {
        UUID sub = UUID.randomUUID();
        createDbUser(sub, "getprofusr", "getprofusr@example.com", "Get Profile User");

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("getprofusr"))
                .andExpect(jsonPath("$.email").value("getprofusr@example.com"));
    }

    // =========================================================================
    // Scenario 6 — Unauthenticated GET /api/users/me → 401
    // =========================================================================

    @Test
    void getProfile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Scenario 7 — PATCH /api/users/me → display_name updated; audit row created
    // =========================================================================

    @Test
    void updateProfile_UpdatesDisplayNameAndCreatesAuditRow() throws Exception {
        UUID sub = UUID.randomUUID();
        createDbUser(sub, "updprofusr", "updprofusr@example.com", "Before Name");

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"After Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("After Name"));

        // Verify display name in DB
        User updated = userRepository.findById(sub).orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("After Name");

        // Verify audit row was created
        int auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE sub = ? AND event_type = 'DISPLAY_NAME_CHANGED'",
                Integer.class, sub.toString());
        assertThat(auditCount).isEqualTo(1);
    }

    // =========================================================================
    // Scenario 8 — PUT avatar → stored; GET /api/users/me/avatar streams it back
    // =========================================================================

    @Test
    void uploadAvatar_ThenGetAvatar_ReturnsStoredImage() throws Exception {
        UUID sub = UUID.randomUUID();
        createDbUser(sub, "avatusr01", "avatusr01@example.com", "Avatar User One");

        byte[] pngBytes = createMinimalPng();

        MockMultipartFile avatarFile = new MockMultipartFile(
                "file", "avatar.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(avatarFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isOk());

        // hasAvatar should be true on profile
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvatar").value(true));

        // GET /api/users/me/avatar should stream image bytes
        mockMvc.perform(get("/api/users/me/avatar")
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    // =========================================================================
    // Scenario 9 — DELETE /api/users/me/avatar → hasAvatar=false on profile
    // =========================================================================

    @Test
    void deleteAvatar_ThenProfileHasAvatarFalse() throws Exception {
        UUID sub = UUID.randomUUID();
        User user = createDbUser(sub, "avatusr02", "avatusr02@example.com", "Avatar User Two");

        // Store an avatar directly in the DB to set up the precondition
        user.setAvatarBytes(createMinimalPng());
        user.setAvatarContentType("image/png");
        userRepository.save(user);

        // Delete the avatar
        mockMvc.perform(delete("/api/users/me/avatar")
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isNoContent());

        // Profile should report hasAvatar=false
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvatar").value(false));
    }

    // =========================================================================
    // Scenario 10 — DELETE /api/users/me → 204; 404 on next GET;
    //               Keycloak user gone; audit rows anonymised (AC#5)
    // =========================================================================

    @Test
    void deleteAccount_Returns204_UserGone_KeycloakGone_AuditAnonymized() throws Exception {
        // Register a user so it exists in both DB and Keycloak
        RegistrationRequest reg = new RegistrationRequest(
                "usrscn10", "usrscn10@example.com", "Str0ng!Pass", "Delete Me");

        String regBody = mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String sub = objectMapper.readTree(regBody).get("sub").asText();

        // Create an audit row by updating the profile
        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Updated Name\"}"))
                .andExpect(status().isOk());

        // Delete the account
        mockMvc.perform(delete("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub))))
                .andExpect(status().isNoContent());

        // GET should now return 404
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(sub))))
                .andExpect(status().isNotFound());

        // DB user row must be gone
        assertThat(userRepository.findById(UUID.fromString(sub))).isEmpty();

        // Keycloak user must be gone
        assertThat(keycloakUserExistsByUsername("usrscn10")).isFalse();

        // AC#5: No audit rows with the original sub; all anonymised to DELETED:…
        int originalSubCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE sub = ?",
                Integer.class, sub);
        assertThat(originalSubCount).isZero();

        int anonymisedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE sub LIKE 'DELETED:%'",
                Integer.class);
        assertThat(anonymisedCount).isPositive();
    }

    // =========================================================================
    // AC#3 — Compensating delete: Keycloak user removed when DB save fails
    // =========================================================================

    @Test
    void register_DbSaveFailsAfterIdpCreate_KeycloakUserDeleted() throws Exception {
        // Pre-insert a user with the target email directly into DB (bypasses Keycloak).
        // This will cause a unique-constraint violation on email when the new user
        // is saved, triggering the compensating delete on the Keycloak side.
        UUID existingUserSub = UUID.randomUUID();
        createDbUser(existingUserSub, "preexist1", "comptest@example.com", "Pre-existing User");

        // Attempt to register a NEW username with the ALREADY-TAKEN email.
        // Flow: existsByUsername("comptest1") = false → OK
        //       createUser("comptest1", "comptest@example.com") in Keycloak → success (email not in KC)
        //       userRepository.save() → FAILS (email unique constraint in DB)
        //       catch: deleteUser(sub) in Keycloak → compensating delete
        //       throws RegistrationException → 500
        RegistrationRequest req = new RegistrationRequest(
                "comptest1", "comptest@example.com", "Str0ng!Pass", "Compensating Test");

        mockMvc.perform(post("/api/register")
                        .header("X-Forwarded-For", "10.1.1.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());

        // Keycloak user must have been deleted by the compensating logic
        assertThat(keycloakUserExistsByUsername("comptest1")).isFalse();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Creates a user row directly in the DB without touching Keycloak.
     * Used for tests that only need an authenticated DB user.
     */
    private User createDbUser(UUID sub, String username, String email, String displayName) {
        User user = new User();
        user.setSub(sub);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        return userRepository.save(user);
    }

    /**
     * Returns a minimal valid 1×1 PNG image as bytes, used for avatar upload tests.
     */
    private static byte[] createMinimalPng() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFFFFFF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /**
     * Returns the base URL for the Keycloak container on the host.
     */
    private static String keycloakBaseUrl() {
        return "http://localhost:" + keycloak.getMappedPort(8080);
    }

    /**
     * Obtains a short-lived admin token from the test Keycloak realm using the
     * {@code backend-admin} service account (client-credentials grant).
     */
    @SuppressWarnings("unchecked")
    private String getKeycloakAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", "backend-admin");
        form.add("client_secret", "backend-admin-secret");

        Map<String, Object> response = RestClient.create()
                .post()
                .uri(keycloakBaseUrl() + "/realms/test/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response).isNotNull().containsKey("access_token");
        return (String) response.get("access_token");
    }

    /**
     * Checks whether a user with the given {@code username} exists in the test Keycloak realm.
     */
    @SuppressWarnings("unchecked")
    private boolean keycloakUserExistsByUsername(String username) {
        String token = getKeycloakAdminToken();

        List<Map<String, Object>> users = RestClient.create()
                .get()
                .uri(keycloakBaseUrl() + "/admin/realms/test/users?username={username}&exact=true",
                        username)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        return users != null && !users.isEmpty();
    }
}
