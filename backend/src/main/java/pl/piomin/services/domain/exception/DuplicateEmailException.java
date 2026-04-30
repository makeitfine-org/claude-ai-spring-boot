package pl.piomin.services.domain.exception;

/**
 * Thrown when a user creation is attempted with an email address that already exists in the identity provider.
 */
public class DuplicateEmailException extends IdentityProviderException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("Email already exists: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
