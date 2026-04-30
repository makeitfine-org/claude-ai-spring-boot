package pl.piomin.services.domain.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateUserCommandTest {

    @Test
    void constructor_StoresAllFields() {
        CreateUserCommand command = new CreateUserCommand("john", "john@example.com", "secret", "John", "Doe");

        assertThat(command.getUsername()).isEqualTo("john");
        assertThat(command.getEmail()).isEqualTo("john@example.com");
        assertThat(command.getPassword()).isEqualTo("secret");
        assertThat(command.getFirstName()).isEqualTo("John");
        assertThat(command.getLastName()).isEqualTo("Doe");
    }

    @Test
    void duplicateUsernameException_ContainsUsername() {
        DuplicateUsernameException ex = new DuplicateUsernameException("john");

        assertThat(ex.getMessage()).contains("john");
    }

    @Test
    void duplicateUsernameException_IsRuntimeException() {
        assertThatThrownBy(() -> { throw new DuplicateUsernameException("test"); })
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("test");
    }
}
