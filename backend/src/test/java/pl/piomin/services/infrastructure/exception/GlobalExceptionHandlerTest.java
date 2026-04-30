package pl.piomin.services.infrastructure.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pl.piomin.services.application.dto.PersonRequest;
import pl.piomin.services.domain.exception.DuplicateEmailException;
import pl.piomin.services.domain.exception.DuplicateUsernameException;
import pl.piomin.services.domain.exception.PasswordPolicyViolationException;
import pl.piomin.services.domain.exception.RegistrationException;
import pl.piomin.services.domain.exception.UserNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlePersonNotFoundException_WithStringMessage() {
        PersonNotFoundException ex = new PersonNotFoundException("Person not found with email: test@example.com");
        ProblemDetail result = handler.handlePersonNotFoundException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Person Not Found");
        assertThat(result.getDetail()).isEqualTo("Person not found with email: test@example.com");
    }

    @Test
    void handlePersonNotFoundException_WithIdConstructor() {
        PersonNotFoundException ex = new PersonNotFoundException(42L);
        ProblemDetail result = handler.handlePersonNotFoundException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Person Not Found");
        assertThat(result.getDetail()).contains("42");
    }

    @Test
    void handleValidationExceptions() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new PersonRequest(), "personRequest");
        bindingResult.addError(new FieldError("personRequest", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail result = handler.handleValidationExceptions(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation Error");
        assertThat(result.getProperties()).containsKey("errors");
    }

    @Test
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ProblemDetail result = handler.handleBadCredentialsException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Authentication Failed");
        assertThat(result.getDetail()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleUsernameNotFoundException() {
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");
        ProblemDetail result = handler.handleUsernameNotFoundException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("User Not Found");
        assertThat(result.getDetail()).isEqualTo("User not found");
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Email already exists");
        ProblemDetail result = handler.handleIllegalArgumentException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
        assertThat(result.getDetail()).isEqualTo("Email already exists");
    }

    @Test
    void handleGlobalException() {
        Exception ex = new RuntimeException("Unexpected failure");
        ProblemDetail result = handler.handleGlobalException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getProperties()).containsKey("message");
    }

    @Test
    void handleDuplicateUsernameException() {
        DuplicateUsernameException ex = new DuplicateUsernameException("john_doe");
        ProblemDetail result = handler.handleDuplicateUsernameException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Duplicate Username");
        assertThat(result.getProperties()).containsKey("errors");
    }

    @Test
    void handleDuplicateEmailException() {
        DuplicateEmailException ex = new DuplicateEmailException("john.doe@example.com");
        ProblemDetail result = handler.handleDuplicateEmailException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Duplicate Email");
        assertThat(result.getProperties()).containsKey("errors");
    }

    @Test
    void handlePasswordPolicyViolationException() {
        PasswordPolicyViolationException ex = new PasswordPolicyViolationException(
                List.of("password must be at least 8 characters long", "password must contain a digit"));
        ProblemDetail result = handler.handlePasswordPolicyViolationException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Password Policy Violation");
        assertThat(result.getProperties()).containsKey("errors");
    }

    @Test
    void handleUserNotFoundException() {
        UUID sub = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserNotFoundException ex = new UserNotFoundException(sub);
        ProblemDetail result = handler.handleUserNotFoundException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("User Not Found");
        assertThat(result.getDetail()).contains("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void handleRegistrationException() {
        RegistrationException ex = new RegistrationException(
                "Registration failed after IdP user creation", new RuntimeException("DB error"));
        ProblemDetail result = handler.handleRegistrationException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Registration Failed");
        assertThat(result.getDetail()).contains("Registration could not be completed");
    }
}
