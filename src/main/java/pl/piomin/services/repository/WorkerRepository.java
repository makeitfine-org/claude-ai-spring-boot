package pl.piomin.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.piomin.services.model.Worker;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
}
