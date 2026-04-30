package pl.piomin.services.domain.exception;

/**
 * Thrown when registration fails after the identity provider user has already been created,
 * indicating that compensating actions (IdP user deletion) have been attempted.
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
