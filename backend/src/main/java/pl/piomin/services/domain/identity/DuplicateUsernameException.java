package pl.piomin.services.domain.identity;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("User already exists with username: " + username);
    }
}
