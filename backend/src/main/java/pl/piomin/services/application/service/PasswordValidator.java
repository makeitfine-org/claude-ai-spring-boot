package pl.piomin.services.application.service;

import org.springframework.stereotype.Component;
import pl.piomin.services.domain.exception.PasswordPolicyViolationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a candidate password against the application password policy.
 *
 * <p>Rules enforced:
 * <ol>
 *   <li>Minimum length of 8 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special (non-alphanumeric) character</li>
 *   <li>Must not equal the username (case-insensitive)</li>
 *   <li>Must not equal the email local-part (case-insensitive)</li>
 * </ol>
 */
@Component
public class PasswordValidator {

    /**
     * Validates the password against all policy rules.
     *
     * @param password the candidate password
     * @param username the username to compare against
     * @param email    the email address whose local-part is compared against the password
     * @throws PasswordPolicyViolationException if any rule is violated
     */
    public void validate(String password, String username, String email) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < 8) {
            violations.add("password must be at least 8 characters long");
        }

        if (password != null) {
            if (!hasUppercase(password)) {
                violations.add("password must contain at least one uppercase letter");
            }
            if (!hasLowercase(password)) {
                violations.add("password must contain at least one lowercase letter");
            }
            if (!hasDigit(password)) {
                violations.add("password must contain at least one digit");
            }
            if (!hasSpecialCharacter(password)) {
                violations.add("password must contain at least one special character");
            }
            if (username != null && password.equalsIgnoreCase(username)) {
                violations.add("password must not be the same as the username");
            }
            if (email != null) {
                String localPart = extractLocalPart(email);
                if (!localPart.isEmpty() && password.equalsIgnoreCase(localPart)) {
                    violations.add("password must not be the same as the email local-part");
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }

    private boolean hasUppercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLowercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSpecialCharacter(String password) {
        for (char c : password.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private String extractLocalPart(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "";
        }
        return email.substring(0, atIndex);
    }
}
