package pl.piomin.services.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import pl.piomin.services.config.JpaAuditingConfig;
import pl.piomin.services.domain.entity.Person;
import pl.piomin.services.domain.repository.PersonRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(JpaAuditingConfig.class)
class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSavePersonSuccessfully() {
        Person person = new Person("John", "Doe", "john.doe@example.com");
        person.setPhoneNumber("+1234567890");
        person.setCity("New York");
        person.setActive(true);

        Person saved = personRepository.save(person);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindPersonByEmail() {
        Person person = new Person("Jane", "Smith", "jane.smith@example.com");
        entityManager.persistAndFlush(person);

        Optional<Person> found = personRepository.findByEmail("jane.smith@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
        assertThat(found.get().getLastName()).isEqualTo("Smith");
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<Person> found = personRepository.findByEmail("notfound@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfEmailExists() {
        Person person = new Person("Bob", "Johnson", "bob.johnson@example.com");
        entityManager.persistAndFlush(person);

        boolean exists = personRepository.existsByEmail("bob.johnson@example.com");
        boolean notExists = personRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldUpdatePersonSuccessfully() {
        Person person = new Person("Alice", "Williams", "alice.williams@example.com");
        person = entityManager.persistAndFlush(person);
        Long originalVersion = person.getVersion();

        person.setPhoneNumber("+9876543210");
        person.setCity("Los Angeles");
        Person updated = personRepository.save(person);
        entityManager.flush();

        assertThat(updated.getPhoneNumber()).isEqualTo("+9876543210");
        assertThat(updated.getCity()).isEqualTo("Los Angeles");
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
    }

    @Test
    void shouldDeletePersonSuccessfully() {
        Person person = new Person("Charlie", "Brown", "charlie.brown@example.com");
        person = entityManager.persistAndFlush(person);
        Long personId = person.getId();

        personRepository.delete(person);
        entityManager.flush();

        Optional<Person> deleted = personRepository.findById(personId);
        assertThat(deleted).isEmpty();
    }

    @Test
    void shouldSearchByFirstNameCaseInsensitive() {
        Person person = new Person("Alice", "Anderson", "alice@example.com");
        entityManager.persistAndFlush(person);

        Page<Person> result = personRepository.search("alic", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Alice");
    }

    @Test
    void shouldSearchByCity() {
        Person person = new Person("Bob", "Builder", "bob@example.com");
        person.setCity("Springfield");
        entityManager.persistAndFlush(person);

        Page<Person> result = personRepository.search("spring", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCity()).isEqualTo("Springfield");
    }

    @Test
    void shouldSearchByEmail() {
        Person person = new Person("Carol", "Clark", "carol.clark@mycompany.com");
        entityManager.persistAndFlush(person);

        Page<Person> result = personRepository.search("mycompany", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("carol.clark@mycompany.com");
    }

    @Test
    void shouldReturnEmptyPageWhenNoMatch() {
        Person person = new Person("Dave", "Doe", "dave@example.com");
        entityManager.persistAndFlush(person);

        Page<Person> result = personRepository.search("xyznotfound", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldReturnAllPersonsWhenQueryIsBlank() {
        entityManager.persistAndFlush(new Person("Eve", "Evans", "eve@example.com"));
        entityManager.persistAndFlush(new Person("Frank", "Fox", "frank@example.com"));

        Page<Person> result = personRepository.search("", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldReturnAllPersonsWhenQueryIsNull() {
        entityManager.persistAndFlush(new Person("Grace", "Green", "grace@example.com"));

        Page<Person> result = personRepository.search(null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldSavePersonWithAllFields() {
        Person person = new Person("David", "Miller", "david.miller@example.com");
        person.setPhoneNumber("+1122334455");
        person.setStreet("123 Main St");
        person.setCity("Chicago");
        person.setPostalCode("60601");
        person.setCountry("USA");
        person.setDateOfBirth(LocalDate.of(1990, 5, 15));
        person.setActive(true);

        Person saved = personRepository.save(person);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStreet()).isEqualTo("123 Main St");
        assertThat(saved.getCity()).isEqualTo("Chicago");
        assertThat(saved.getPostalCode()).isEqualTo("60601");
        assertThat(saved.getCountry()).isEqualTo("USA");
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(saved.isActive()).isTrue();
    }
}
