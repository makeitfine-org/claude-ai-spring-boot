package pl.piomin.services.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.dto.WorkerRequest;
import pl.piomin.services.exception.ResourceNotFoundException;
import pl.piomin.services.model.Worker;
import pl.piomin.services.repository.WorkerRepository;

import java.util.List;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Transactional(readOnly = true)
    public List<Worker> findAll() {
        return workerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Worker findById(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + id));
    }

    @Transactional
    public Worker create(WorkerRequest request) {
        Worker worker = new Worker();
        worker.setName(request.name());
        worker.setSurname(request.surname());
        worker.setAge(request.age());
        return workerRepository.save(worker);
    }

    @Transactional
    public Worker update(Long id, WorkerRequest request) {
        Worker worker = findById(id);
        worker.setName(request.name());
        worker.setSurname(request.surname());
        worker.setAge(request.age());
        return workerRepository.save(worker);
    }

    @Transactional
    public void delete(Long id) {
        if (!workerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Worker not found: " + id);
        }
        workerRepository.deleteById(id);
    }
}
