package pl.piomin.services.domain.exception;

import java.util.List;

/**
 * Thrown when a candidate password fails one or more password policy rules.
 */
public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super("Password policy violated: " + String.join(", ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
