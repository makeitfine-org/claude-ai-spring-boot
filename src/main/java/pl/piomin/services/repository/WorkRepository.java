package pl.piomin.services.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.piomin.services.model.Work;

import java.util.List;
import java.util.Optional;

public interface WorkRepository extends JpaRepository<Work, Long> {

    @EntityGraph(attributePaths = "worker")
    List<Work> findAll();

    @Query("SELECT w FROM Work w JOIN FETCH w.worker WHERE w.id = :id")
    Optional<Work> findByIdWithWorker(@Param("id") Long id);
}
