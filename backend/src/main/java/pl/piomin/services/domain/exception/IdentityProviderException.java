package pl.piomin.services.domain.exception;

/**
 * Base runtime exception for all identity provider failures.
 */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String message) {
        super(message);
    }

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
