package pl.piomin.services.domain.port;

/**
 * Command object carrying the data required to create a user in the identity provider.
 */
public class CreateUserCommand {

    private final String username;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public CreateUserCommand(String username, String email, String password,
                             String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
