package pl.piomin.services.domain.exception;

public class AvatarTooLargeException extends RuntimeException {

    private final long actualSize;
    private final long maxSize;

    public AvatarTooLargeException(long actualSize, long maxSize) {
        super("Avatar file size " + actualSize + " bytes exceeds the maximum allowed size of " + maxSize + " bytes");
        this.actualSize = actualSize;
        this.maxSize = maxSize;
    }

    public long getActualSize() {
        return actualSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}
