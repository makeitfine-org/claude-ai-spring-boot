package pl.piomin.services.application.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    // -------------------------------------------------------------------------
    // AuthRequest
    // -------------------------------------------------------------------------

    @Test
    void authRequest_NoArgConstructor_FieldsAreNull() {
        AuthRequest request = new AuthRequest();
        assertThat(request.getEmail()).isNull();
        assertThat(request.getPassword()).isNull();
    }

    @Test
    void authRequest_AllArgConstructor_FieldsSet() {
        AuthRequest request = new AuthRequest("user@example.com", "secret");
        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("secret");
    }

    @Test
    void authRequest_Setters_UpdateFields() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setPassword("newpass");
        assertThat(request.getEmail()).isEqualTo("new@example.com");
        assertThat(request.getPassword()).isEqualTo("newpass");
    }

    // -------------------------------------------------------------------------
    // AuthResponse
    // -------------------------------------------------------------------------

    @Test
    void authResponse_NoArgConstructor_TokenTypeIsBearer() {
        AuthResponse response = new AuthResponse();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
    }

    @Test
    void authResponse_AllArgConstructor_FieldsSet() {
        AuthResponse response = new AuthResponse("access123", "refresh456");
        assertThat(response.getAccessToken()).isEqualTo("access123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void authResponse_Setters_UpdateFields() {
        AuthResponse response = new AuthResponse();
        response.setAccessToken("newAccess");
        response.setRefreshToken("newRefresh");
        response.setTokenType("JWT");
        assertThat(response.getAccessToken()).isEqualTo("newAccess");
        assertThat(response.getRefreshToken()).isEqualTo("newRefresh");
        assertThat(response.getTokenType()).isEqualTo("JWT");
    }

    // -------------------------------------------------------------------------
    // PersonRequest
    // -------------------------------------------------------------------------

    @Test
    void personRequest_NoArgConstructor_ActiveDefaultsTrue() {
        PersonRequest request = new PersonRequest();
        assertThat(request.isActive()).isTrue();
        assertThat(request.getFirstName()).isNull();
        assertThat(request.getLastName()).isNull();
        assertThat(request.getEmail()).isNull();
    }

    @Test
    void personRequest_Setters_UpdateAllFields() {
        PersonRequest request = new PersonRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPhoneNumber("+1234567890");
        request.setStreet("Main St 1");
        request.setCity("New York");
        request.setPostalCode("10001");
        request.setCountry("US");
        request.setDateOfBirth(LocalDate.of(1990, 1, 15));
        request.setActive(false);

        assertThat(request.getFirstName()).isEqualTo("John");
        assertThat(request.getLastName()).isEqualTo("Doe");
        assertThat(request.getEmail()).isEqualTo("john@example.com");
        assertThat(request.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(request.getStreet()).isEqualTo("Main St 1");
        assertThat(request.getCity()).isEqualTo("New York");
        assertThat(request.getPostalCode()).isEqualTo("10001");
        assertThat(request.getCountry()).isEqualTo("US");
        assertThat(request.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(request.isActive()).isFalse();
    }

    // -------------------------------------------------------------------------
    // PersonResponse
    // -------------------------------------------------------------------------

    @Test
    void personResponse_NoArgConstructor_AllFieldsNull() {
        PersonResponse response = new PersonResponse();
        assertThat(response.getId()).isNull();
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
        assertThat(response.getVersion()).isNull();
    }

    @Test
    void personResponse_Setters_UpdateAllFields() {
        PersonResponse response = new PersonResponse();
        LocalDateTime now = LocalDateTime.now();

        response.setId(42L);
        response.setFirstName("Jane");
        response.setLastName("Smith");
        response.setEmail("jane@example.com");
        response.setPhoneNumber("+9876543210");
        response.setStreet("Oak Ave 5");
        response.setCity("Boston");
        response.setPostalCode("02101");
        response.setCountry("US");
        response.setDateOfBirth(LocalDate.of(1985, 6, 20));
        response.setActive(true);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setVersion(1L);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("+9876543210");
        assertThat(response.getStreet()).isEqualTo("Oak Ave 5");
        assertThat(response.getCity()).isEqualTo("Boston");
        assertThat(response.getPostalCode()).isEqualTo("02101");
        assertThat(response.getCountry()).isEqualTo("US");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 6, 20));
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
        assertThat(response.getVersion()).isEqualTo(1L);
    }
}
