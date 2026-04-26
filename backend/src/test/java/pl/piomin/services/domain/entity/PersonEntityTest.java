package pl.piomin.services.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PersonEntityTest {

    @Test
    void noArgConstructor_SetsDefaultActive() {
        Person person = new Person();
        assertThat(person.getActive()).isTrue();
    }

    @Test
    void threeArgConstructor_SetsRequiredFields() {
        Person person = new Person("John", "Doe", "john@example.com");
        assertThat(person.getFirstName()).isEqualTo("John");
        assertThat(person.getLastName()).isEqualTo("Doe");
        assertThat(person.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void settersAndGetters_AllFields() {
        Person person = new Person();
        LocalDate dob = LocalDate.of(1990, 1, 15);
        LocalDateTime now = LocalDateTime.now();

        person.setId(1L);
        person.setFirstName("Jane");
        person.setLastName("Smith");
        person.setEmail("jane@example.com");
        person.setPhoneNumber("+48123456789");
        person.setStreet("Main St 1");
        person.setCity("Warsaw");
        person.setPostalCode("00-001");
        person.setCountry("Poland");
        person.setDateOfBirth(dob);
        person.setActive(false);
        person.setCreatedAt(now);
        person.setUpdatedAt(now);
        person.setVersion(2L);

        assertThat(person.getId()).isEqualTo(1L);
        assertThat(person.getFirstName()).isEqualTo("Jane");
        assertThat(person.getLastName()).isEqualTo("Smith");
        assertThat(person.getEmail()).isEqualTo("jane@example.com");
        assertThat(person.getPhoneNumber()).isEqualTo("+48123456789");
        assertThat(person.getStreet()).isEqualTo("Main St 1");
        assertThat(person.getCity()).isEqualTo("Warsaw");
        assertThat(person.getPostalCode()).isEqualTo("00-001");
        assertThat(person.getCountry()).isEqualTo("Poland");
        assertThat(person.getDateOfBirth()).isEqualTo(dob);
        assertThat(person.getActive()).isFalse();
        assertThat(person.getCreatedAt()).isEqualTo(now);
        assertThat(person.getUpdatedAt()).isEqualTo(now);
        assertThat(person.getVersion()).isEqualTo(2L);
    }

    @Test
    void equals_SameObject_ReturnsTrue() {
        Person person = new Person("John", "Doe", "john@example.com");
        assertThat(person).isEqualTo(person);
    }

    @Test
    void equals_SameIdAndEmail_ReturnsTrue() {
        Person p1 = new Person("John", "Doe", "john@example.com");
        p1.setId(1L);
        Person p2 = new Person("Jane", "Smith", "john@example.com");
        p2.setId(1L);
        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void equals_NullOrDifferentType_ReturnsFalse() {
        Person person = new Person("John", "Doe", "john@example.com");
        assertThat(person).isNotEqualTo(null);
        assertThat(person).isNotEqualTo("string");
    }

    @Test
    void hashCode_ConsistentWithEquals() {
        Person p1 = new Person("John", "Doe", "john@example.com");
        p1.setId(1L);
        Person p2 = new Person("Jane", "Smith", "john@example.com");
        p2.setId(1L);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    void toString_ContainsKeyFields() {
        Person person = new Person("John", "Doe", "john@example.com");
        person.setId(1L);
        String result = person.toString();
        assertThat(result).contains("John").contains("Doe").contains("john@example.com");
    }
}
