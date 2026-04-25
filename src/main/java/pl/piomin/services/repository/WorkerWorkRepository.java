package pl.piomin.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.piomin.services.model.WorkerWork;

import java.util.List;

public interface WorkerWorkRepository extends JpaRepository<WorkerWork, Long> {

    void deleteByWork_Id(Long workId);

    List<WorkerWork> findByWork_Id(Long workId);
}
