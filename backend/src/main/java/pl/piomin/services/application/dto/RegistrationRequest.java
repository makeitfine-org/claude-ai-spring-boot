package pl.piomin.services.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.piomin.services.application.validation.NoControlCharacters;

public class RegistrationRequest {

    @NotBlank(message = "username must not be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username must contain only letters, digits, or underscores")
    @Size(min = 5, max = 15, message = "username must be between 5 and 15 characters")
    private String username;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password must not be blank")
    private String password;

    @NotBlank(message = "displayName must not be blank")
    @Size(min = 1, max = 50, message = "displayName must be between 1 and 50 characters")
    @NoControlCharacters(message = "displayName must not contain control characters")
    private String displayName;

    public RegistrationRequest() {
    }

    public RegistrationRequest(String username, String email, String password, String displayName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
