package pl.piomin.services.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.dto.WorkRequest;
import pl.piomin.services.exception.ResourceNotFoundException;
import pl.piomin.services.model.Work;
import pl.piomin.services.model.Worker;
import pl.piomin.services.model.WorkerWork;
import pl.piomin.services.repository.WorkRepository;
import pl.piomin.services.repository.WorkerRepository;
import pl.piomin.services.repository.WorkerWorkRepository;

import java.util.List;

@Service
public class WorkService {

    private final WorkRepository workRepository;
    private final WorkerRepository workerRepository;
    private final WorkerWorkRepository workerWorkRepository;

    public WorkService(WorkRepository workRepository,
                       WorkerRepository workerRepository,
                       WorkerWorkRepository workerWorkRepository) {
        this.workRepository = workRepository;
        this.workerRepository = workerRepository;
        this.workerWorkRepository = workerWorkRepository;
    }

    @Transactional(readOnly = true)
    public List<Work> findAll() {
        return workRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Work findById(Long id) {
        return workRepository.findByIdWithWorker(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work not found: " + id));
    }

    @Transactional
    public Work create(WorkRequest request) {
        Worker assignedWorker = workerRepository.findById(request.assignedWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + request.assignedWorkerId()));

        Work work = new Work();
        work.setTitle(request.title());
        work.setDescription(request.description());
        work.setEndDate(request.endDate());
        work.setPrice(request.price());
        work.setPayDate(request.payDate());
        work.setWorker(assignedWorker);
        Work saved = workRepository.save(work);

        if (request.additionalWorkerIds() != null) {
            for (Long workerId : request.additionalWorkerIds()) {
                Worker additional = workerRepository.findById(workerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));
                workerWorkRepository.save(new WorkerWork(additional, saved));
            }
        }
        return saved;
    }

    @Transactional
    public Work update(Long id, WorkRequest request) {
        Work work = findById(id);
        Worker assignedWorker = workerRepository.findById(request.assignedWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + request.assignedWorkerId()));

        work.setTitle(request.title());
        work.setDescription(request.description());
        work.setEndDate(request.endDate());
        work.setPrice(request.price());
        work.setPayDate(request.payDate());
        work.setWorker(assignedWorker);
        return workRepository.save(work);
    }

    @Transactional
    public void delete(Long id) {
        if (!workRepository.existsById(id)) {
            throw new ResourceNotFoundException("Work not found: " + id);
        }
        workerWorkRepository.deleteByWork_Id(id);
        workRepository.deleteById(id);
    }
}
