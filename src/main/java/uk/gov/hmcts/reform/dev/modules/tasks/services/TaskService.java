package uk.gov.hmcts.reform.dev.modules.tasks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateStatusDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.modules.tasks.models.Task;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;
import uk.gov.hmcts.reform.dev.modules.tasks.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public ResponseDTO createTask(CreationDTO dto) {
        logger.info("Creating task with title: {}", dto.getTitle());

        try {
            Task task = new Task();
            task.setTitle(dto.getTitle());
            task.setDescription(dto.getDescription());
            task.setStatus(TaskStatus.PENDING);
            task.setDueDate(LocalDateTime.parse(dto.getDueDate()));

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

    public List<ResponseDTO> getAllTasks() {
        logger.debug("Fetching all tasks");
        List<Task> tasks = taskRepository.findAllByOrderByDueDateAsc();
        logger.debug("Found {} tasks", tasks.size());
        return tasks.stream()
            .map(ResponseDTO::fromTask)
            .collect(Collectors.toList());
    }

    public List<ResponseDTO> getAllTasksByStatus(TaskStatus status) {
        logger.debug("Fetching tasks with status: {}", status);
        List<Task> tasks = taskRepository.findAllByStatusOrderByDueDateAsc(status);
        logger.debug("Found {} tasks with status {}", tasks.size(), status);
        return tasks.stream()
            .map(ResponseDTO::fromTask)
            .collect(Collectors.toList());
    }
}
