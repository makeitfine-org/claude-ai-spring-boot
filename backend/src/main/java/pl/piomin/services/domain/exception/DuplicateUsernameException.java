package pl.piomin.services.domain.exception;

/**
 * Thrown when a user creation is attempted with a username that already exists in the identity provider.
 */
public class DuplicateUsernameException extends IdentityProviderException {

    private final String username;

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
