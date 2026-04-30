package pl.piomin.services.application.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoControlCharactersValidatorTest {

    private NoControlCharactersValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NoControlCharactersValidator();
    }

    // -------------------------------------------------------------------------
    // Valid cases
    // -------------------------------------------------------------------------

    @Test
    void isValid_NullValue_ReturnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_EmptyString_ReturnsTrue() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    void isValid_NormalAsciiString_ReturnsTrue() {
        assertThat(validator.isValid("John Doe", context)).isTrue();
    }

    @Test
    void isValid_StringWithUnicodeLetters_ReturnsTrue() {
        assertThat(validator.isValid("Joehn Doee", context)).isTrue();
    }

    @Test
    void isValid_StringWithPrintableAsciiSymbols_ReturnsTrue() {
        assertThat(validator.isValid("ABCabc123 !@#$%^&*()", context)).isTrue();
    }

    @Test
    void isValid_SpaceCharacterIsNotControlChar_ReturnsTrue() {
        // Space = 0x20, which is above 0x1F — should be valid
        assertThat(validator.isValid("John Doe", context)).isTrue();
    }

    // -------------------------------------------------------------------------
    // Invalid cases — control characters in range [0x00, 0x1F]
    // -------------------------------------------------------------------------

    @Test
    void isValid_StringWithNulByte_ReturnsFalse() {
        // NUL = 0x00
        String withNul = "John" + (char) 0x00 + "Doe";
        assertThat(validator.isValid(withNul, context)).isFalse();
    }

    @Test
    void isValid_StringWithSoh_ReturnsFalse() {
        // SOH = 0x01
        String withSoh = "John" + (char) 0x01 + "Doe";
        assertThat(validator.isValid(withSoh, context)).isFalse();
    }

    @Test
    void isValid_StringWithTab_ReturnsFalse() {
        // HT = 0x09
        assertThat(validator.isValid("John\tDoe", context)).isFalse();
    }

    @Test
    void isValid_StringWithNewline_ReturnsFalse() {
        // LF = 0x0A
        assertThat(validator.isValid("John\nDoe", context)).isFalse();
    }

    @Test
    void isValid_StringWithCarriageReturn_ReturnsFalse() {
        // CR = 0x0D
        assertThat(validator.isValid("John\rDoe", context)).isFalse();
    }

    @Test
    void isValid_StringWithEscapeChar_ReturnsFalse() {
        // ESC = 0x1B
        String withEsc = "JohnDoe" + (char) 0x1B;
        assertThat(validator.isValid(withEsc, context)).isFalse();
    }

    @Test
    void isValid_StringWithUs_ReturnsFalse() {
        // US = 0x1F (last ASCII control char before space)
        String withUs = (char) 0x1F + "JohnDoe";
        assertThat(validator.isValid(withUs, context)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Invalid cases — DEL = 0x7F
    // -------------------------------------------------------------------------

    @Test
    void isValid_StringWithDeleteChar_ReturnsFalse() {
        // DEL = 0x7F
        String withDel = "JohnDoe" + (char) 0x7F;
        assertThat(validator.isValid(withDel, context)).isFalse();
    }
}
