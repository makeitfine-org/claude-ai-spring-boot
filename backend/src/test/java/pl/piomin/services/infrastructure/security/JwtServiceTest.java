package pl.piomin.services.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Base64-encoded key long enough for HS256 (256+ bits)
    private static final String SECRET =
            "dGVzdHNlY3JldGtleXRlc3RzZWNyZXRrZXl0ZXN0c2VjcmV0a2V5dGVzdA==";
    private static final long EXPIRATION = 3_600_000L;       // 1 hour
    private static final long REFRESH_EXPIRATION = 86_400_000L; // 24 hours
    private static final long EXPIRED = -1_000L;             // already expired

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // generateToken / extractUsername
    // -------------------------------------------------------------------------

    @Test
    void generateToken_NoExtraClaims_ExtractsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    void generateToken_WithExtraClaims_ExtractsCorrectUsername() {
        Map<String, Object> extraClaims = Map.of("role", "admin");
        String token = jwtService.generateToken(extraClaims, userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
    }

    // -------------------------------------------------------------------------
    // generateRefreshToken
    // -------------------------------------------------------------------------

    @Test
    void generateRefreshToken_ExtractsCorrectUsername() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("test@example.com");
    }

    // -------------------------------------------------------------------------
    // extractClaim
    // -------------------------------------------------------------------------

    @Test
    void extractClaim_SubjectClaim_ReturnsUsername() {
        String token = jwtService.generateToken(userDetails);

        String subject = jwtService.extractClaim(token,
                claims -> claims.getSubject());

        assertThat(subject).isEqualTo("test@example.com");
    }

    // -------------------------------------------------------------------------
    // isTokenValid
    // -------------------------------------------------------------------------

    @Test
    void isTokenValid_ValidToken_SameUser_ReturnsTrue() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_ValidToken_DifferentUser_ReturnsFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "password", Collections.emptyList());

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_ExpiredToken_ThrowsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRED);
        String expiredToken = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // -------------------------------------------------------------------------
    // extractUsername — invalid token
    // -------------------------------------------------------------------------

    @Test
    void extractUsername_InvalidToken_ThrowsException() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
