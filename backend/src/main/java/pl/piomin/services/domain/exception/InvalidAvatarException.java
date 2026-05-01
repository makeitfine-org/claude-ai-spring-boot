package pl.piomin.services.domain.exception;

public class InvalidAvatarException extends RuntimeException {

    public InvalidAvatarException(String message) {
        super(message);
    }
}
