package pl.piomin.services.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void noArgConstructor_CreatesEmptyUser() {
        User user = new User();
        assertThat(user.getSub()).isNull();
        assertThat(user.getUsername()).isNull();
    }

    @Test
    void settersAndGetters_AllFields() {
        UUID sub = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        byte[] avatar = {1, 2, 3};

        User user = new User();
        user.setSub(sub);
        user.setUsername("john_doe");
        user.setDisplayName("John Doe");
        user.setEmail("john@example.com");
        user.setAvatarBytes(avatar);
        user.setAvatarContentType("image/png");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertThat(user.getSub()).isEqualTo(sub);
        assertThat(user.getUsername()).isEqualTo("john_doe");
        assertThat(user.getDisplayName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getAvatarBytes()).isEqualTo(avatar);
        assertThat(user.getAvatarContentType()).isEqualTo("image/png");
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void equals_SameObject_ReturnsTrue() {
        User user = new User();
        user.setSub(UUID.randomUUID());
        assertThat(user).isEqualTo(user);
    }

    @Test
    void equals_SameSubAndEmail_ReturnsTrue() {
        UUID sub = UUID.randomUUID();
        User u1 = new User();
        u1.setSub(sub);
        u1.setEmail("john@example.com");
        User u2 = new User();
        u2.setSub(sub);
        u2.setEmail("john@example.com");
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    void equals_NullOrDifferentType_ReturnsFalse() {
        User user = new User();
        user.setSub(UUID.randomUUID());
        assertThat(user).isNotEqualTo(null);
        assertThat(user).isNotEqualTo("string");
    }

    @Test
    void hashCode_ConsistentWithEquals() {
        UUID sub = UUID.randomUUID();
        User u1 = new User();
        u1.setSub(sub);
        u1.setEmail("john@example.com");
        User u2 = new User();
        u2.setSub(sub);
        u2.setEmail("john@example.com");
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void toString_ContainsKeyFields() {
        UUID sub = UUID.randomUUID();
        User user = new User();
        user.setSub(sub);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        String result = user.toString();
        assertThat(result).contains("john_doe").contains("john@example.com");
    }
}
