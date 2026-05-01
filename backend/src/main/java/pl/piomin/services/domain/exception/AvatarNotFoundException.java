package pl.piomin.services.domain.exception;

import java.util.UUID;

public class AvatarNotFoundException extends RuntimeException {

    private final UUID sub;

    public AvatarNotFoundException(UUID sub) {
        super("No avatar found for user: " + sub);
        this.sub = sub;
    }

    public UUID getSub() {
        return sub;
    }
}
