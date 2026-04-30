package pl.piomin.services.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a String field contains no Unicode control characters (code points 0x00–0x1F and 0x7F).
 * Null values are considered valid; combine with {@code @NotBlank} when null must be rejected.
 */
@Documented
@Constraint(validatedBy = NoControlCharactersValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoControlCharacters {

    String message() default "must not contain control characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
