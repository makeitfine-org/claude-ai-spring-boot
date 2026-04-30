package pl.piomin.services.domain.port;

/**
 * Port that abstracts all Identity Provider operations.
 * No application code outside the adapter may use IdP-specific APIs.
 */
public interface IdentityProvider {

    /**
     * Creates a user in the identity provider.
     *
     * @param command the user creation command
     * @return the IdP subject identifier (sub) for the created user
     */
    String createUser(CreateUserCommand command);

    /**
     * Deletes a user from the identity provider by their subject identifier.
     *
     * @param sub the IdP subject identifier
     */
    void deleteUser(String sub);

    /**
     * Triggers an email verification for the user identified by the given subject.
     *
     * @param sub the IdP subject identifier
     */
    void triggerEmailVerification(String sub);
}
