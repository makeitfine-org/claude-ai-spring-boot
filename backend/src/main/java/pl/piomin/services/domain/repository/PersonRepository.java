package pl.piomin.services.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.piomin.services.domain.entity.Person;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT p FROM Person p
            WHERE :q IS NULL OR :q = ''
               OR LOWER(p.firstName)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.lastName)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.email)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.city)        LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Person> search(@Param("q") String q, Pageable pageable);

}
