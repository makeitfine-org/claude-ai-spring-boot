package pl.piomin.services.domain.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotFoundExceptionTest {

    @Test
    void constructor_SetsMessageWithSub() {
        UUID sub = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserNotFoundException ex = new UserNotFoundException(sub);

        assertThat(ex.getMessage()).isEqualTo("User not found: 550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void getSub_ReturnsSub() {
        UUID sub = UUID.randomUUID();
        UserNotFoundException ex = new UserNotFoundException(sub);

        assertThat(ex.getSub()).isEqualTo(sub);
    }

    @Test
    void isRuntimeException() {
        UUID sub = UUID.randomUUID();
        UserNotFoundException ex = new UserNotFoundException(sub);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
