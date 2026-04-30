package pl.piomin.services.domain.identity;

public interface IdentityProvider {
    String createUser(CreateUserCommand command);
    void deleteUser(String sub);
    void triggerEmailVerification(String sub);
}
