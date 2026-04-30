package pl.piomin.services.application.dto;

import jakarta.validation.constraints.Size;
import pl.piomin.services.application.validation.NoControlCharacters;

public class UpdateProfileRequest {

    @Size(min = 1, max = 50)
    @NoControlCharacters
    private String displayName;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
