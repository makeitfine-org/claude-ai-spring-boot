package pl.piomin.services.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.piomin.services.domain.exception.PasswordPolicyViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void validate_ValidPassword_DoesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                passwordValidator.validate("Str0ng!Pass", "john_doe", "john.doe@example.com"));
    }

    // -------------------------------------------------------------------------
    // Rule 1: minimum length
    // -------------------------------------------------------------------------

    @Test
    void validate_TooShortPassword_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate("Sh0rt!", "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void validate_ExactlyEightCharacters_DoesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                passwordValidator.validate("Abcde1!x", "john_doe", "john.doe@example.com"));
    }

    // -------------------------------------------------------------------------
    // Rule 2: uppercase letter
    // -------------------------------------------------------------------------

    @Test
    void validate_NoUppercase_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate("str0ng!pass", "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("uppercase");
    }

    // -------------------------------------------------------------------------
    // Rule 3: lowercase letter
    // -------------------------------------------------------------------------

    @Test
    void validate_NoLowercase_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate("STR0NG!PASS", "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("lowercase");
    }

    // -------------------------------------------------------------------------
    // Rule 4: digit
    // -------------------------------------------------------------------------

    @Test
    void validate_NoDigit_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate("Strong!Pass", "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("digit");
    }

    // -------------------------------------------------------------------------
    // Rule 5: special character
    // -------------------------------------------------------------------------

    @Test
    void validate_NoSpecialCharacter_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate("Str0ngPass", "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("special character");
    }

    // -------------------------------------------------------------------------
    // Rule 6: password must not equal username (case-insensitive)
    // -------------------------------------------------------------------------

    @Test
    void validate_PasswordEqualsUsername_ThrowsPasswordPolicyViolation() {
        // "John1!doe" passes length/upper/lower/digit/special — only fails rule 6 (equals username)
        assertThatThrownBy(() ->
                passwordValidator.validate("John1!doe", "john1!doe", "other@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("username");
    }

    @Test
    void validate_PasswordEqualsUsernameCaseInsensitive_ThrowsPasswordPolicyViolation() {
        // Mixed-case version that passes rules 1-5 but still equals username case-insensitively
        assertThatThrownBy(() ->
                passwordValidator.validate("JoHn1!dOe", "john1!doe", "other@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("username");
    }

    // -------------------------------------------------------------------------
    // Rule 7: password must not equal email local-part (case-insensitive)
    // -------------------------------------------------------------------------

    @Test
    void validate_PasswordEqualsEmailLocalPart_ThrowsPasswordPolicyViolation() {
        // localpart = "john1doe" — has uppercase J, lowercase, digit, special char . => passes 1-5, fails rule 7
        assertThatThrownBy(() ->
                passwordValidator.validate("John1!doe", "other_user", "john1!doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("email local-part");
    }

    @Test
    void validate_PasswordEqualsEmailLocalPartCaseInsensitive_ThrowsPasswordPolicyViolation() {
        // Mixed-case version that passes rules 1-5 but still equals email local-part case-insensitively
        assertThatThrownBy(() ->
                passwordValidator.validate("JoHn1!dOe", "other_user", "john1!doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("email local-part");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void validate_NullPassword_ThrowsPasswordPolicyViolation() {
        assertThatThrownBy(() ->
                passwordValidator.validate(null, "john_doe", "john.doe@example.com"))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void validate_NullUsername_DoesNotCheckUsernameRule() {
        assertThatNoException().isThrownBy(() ->
                passwordValidator.validate("Str0ng!Pass", null, "john.doe@example.com"));
    }

    @Test
    void validate_NullEmail_DoesNotCheckEmailRule() {
        assertThatNoException().isThrownBy(() ->
                passwordValidator.validate("Str0ng!Pass", "john_doe", null));
    }

    @Test
    void validate_EmailWithoutAtSign_DoesNotCheckEmailRule() {
        assertThatNoException().isThrownBy(() ->
                passwordValidator.validate("Str0ng!Pass", "john_doe", "invalidemail"));
    }

    @Test
    void validate_MultipleViolations_ReportsAll() {
        // "short" (5 chars, all lowercase, no digit, no special) — violates rules 1, 2, 4, 5
        PasswordPolicyViolationException thrown = null;
        try {
            passwordValidator.validate("short", "john_doe", "john.doe@example.com");
        } catch (PasswordPolicyViolationException ex) {
            thrown = ex;
        }
        assertThat(thrown).isNotNull();
        assertThat(thrown.getViolations()).hasSizeGreaterThan(1);
    }
}
