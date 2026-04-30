package pl.piomin.services.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    private final UUID sub;

    public UserNotFoundException(UUID sub) {
        super("User not found: " + sub);
        this.sub = sub;
    }

    public UUID getSub() {
        return sub;
    }
}
