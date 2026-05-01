package pl.piomin.services.domain.exception;

public class UnsupportedAvatarTypeException extends RuntimeException {

    private final String contentType;

    public UnsupportedAvatarTypeException(String contentType) {
        super("Unsupported avatar content type: " + contentType + ". Only image/jpeg and image/png are allowed");
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
