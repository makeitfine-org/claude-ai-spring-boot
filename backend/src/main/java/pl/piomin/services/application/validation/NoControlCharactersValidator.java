package pl.piomin.services.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Rejects strings that contain ASCII control characters (0x00–0x1F, 0x7F).
 */
public class NoControlCharactersValidator implements ConstraintValidator<NoControlCharacters, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) {
                return false;
            }
        }
        return true;
    }
}
