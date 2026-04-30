package pl.piomin.services.application.dto;

public class RegistrationResponse {

    private String sub;
    private String username;
    private String email;

    public RegistrationResponse() {
    }

    public RegistrationResponse(String sub, String username, String email) {
        this.sub = sub;
        this.username = username;
        this.email = email;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
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
}
