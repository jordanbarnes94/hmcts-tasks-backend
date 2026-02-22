package uk.gov.hmcts.reform.dev.modules.tasks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateStatusDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.modules.tasks.models.Task;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;
import uk.gov.hmcts.reform.dev.modules.tasks.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.modules.tasks.specifications.TaskSpecifications;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    private LocalDateTime parseDateTime(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
    }

    public ResponseDTO createTask(CreationDTO dto) {
        logger.info("Creating task with title: {}", dto.getTitle());

        try {
            Task task = new Task();
            task.setTitle(dto.getTitle());
            task.setDescription(dto.getDescription());
            task.setStatus(TaskStatus.PENDING);
            task.setDueDate(parseDateTime(dto.getDueDate()));

            Task savedTask = taskRepository.save(task);
            logger.info("Task created successfully with ID: {}", savedTask.getId());
            return ResponseDTO.fromTask(savedTask);
        } catch (Exception e) {
            logger.error("Error creating task: {}", e.getMessage(), e);
            throw e;
        }
    }

    public ResponseDTO getTask(Long id) {
        logger.debug("Fetching task with ID: {}", id);
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> {
                logger.warn("Task not found with ID: {}", id);
                return new TaskNotFoundException(id);
            });
        return ResponseDTO.fromTask(task);
    }

    public ResponseDTO updateTask(Long id, UpdateDTO dto) {
        logger.info("Updating task ID: {}", id);

        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(parseDateTime(dto.getDueDate()));
        task.setStatus(dto.getStatus());
        Task updatedTask = taskRepository.save(task);

        logger.info("Task updated successfully for ID: {}", id);
        return ResponseDTO.fromTask(updatedTask);
    }

    public ResponseDTO updateStatus(Long id, UpdateStatusDTO dto) {
        logger.info("Updating status for task ID: {} to {}", id, dto.getStatus());

        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        task.setStatus(dto.getStatus());
        Task updatedTask = taskRepository.save(task);

        logger.info("Task status updated successfully for ID: {}", id);
        return ResponseDTO.fromTask(updatedTask);
    }

    public void deleteTask(Long id) {
        logger.info("Deleting task with ID: {}", id);

        if (!taskRepository.existsById(id)) {
            logger.warn("Attempted to delete non-existent task with ID: {}", id);
            throw new TaskNotFoundException(id);
        }

        taskRepository.deleteById(id);
        logger.info("Task deleted successfully with ID: {}", id);
    }

    public Page<ResponseDTO> getAllTasks(
        TaskStatus status,
        String search,
        LocalDateTime dueDateFrom,
        LocalDateTime dueDateTo,
        Pageable pageable
    ) {
        logger.debug("Fetching tasks with filters - status: {}, search: {}, dueDateFrom: {}, dueDateTo: {}, page: {}",
                     status, search, dueDateFrom, dueDateTo, pageable.getPageNumber());

        Specification<Task> spec = TaskSpecifications.withFilters(status, search, dueDateFrom, dueDateTo);
        Page<Task> tasks = taskRepository.findAll(spec, pageable);

        logger.debug("Found {} tasks (page {} of {})",
                     tasks.getNumberOfElements(),
                     tasks.getNumber() + 1,
                     tasks.getTotalPages());

        return tasks.map(ResponseDTO::fromTask);
    }

}
