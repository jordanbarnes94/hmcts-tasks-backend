package uk.gov.hmcts.reform.dev.modules.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateStatusDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.modules.tasks.models.Task;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;
import uk.gov.hmcts.reform.dev.modules.tasks.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.modules.tasks.services.TaskService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class IntegrationTests {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldCreateTaskAndPersistToDatabase() {
        // Arrange
        String title = "Integration Test Task";
        String description = "Testing database persistence";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO dto = new CreationDTO(title, description, dueDate);

        // Act
        ResponseDTO created = taskService.createTask(dto);

        // Assert - verify task was created with correct data
        assertNotNull(created.getId());
        assertEquals(title, created.getTitle());
        assertEquals(description, created.getDescription());
        assertEquals(TaskStatus.PENDING, created.getStatus());
        assertEquals(LocalDateTime.parse(dueDate), created.getDueDate());

        // Assert - verify task exists in database
        Optional<Task> found = taskRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(title, found.get().getTitle());
        assertEquals(description, found.get().getDescription());
    }

    @Test
    void shouldRetrieveTaskFromDatabase() {
        // Arrange
        String title = "Existing Task";
        String description = "Already in database";
        String dueDate = "2026-01-20T10:00:00";

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(LocalDateTime.parse(dueDate));
        Task saved = taskRepository.save(task);

        // Act
        ResponseDTO retrieved = taskService.getTask(saved.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(title, retrieved.getTitle());
        assertEquals(description, retrieved.getDescription());
        assertEquals(TaskStatus.PENDING, retrieved.getStatus());
    }

    @Test
    void shouldUpdateTaskStatusInDatabase() {
        // Arrange
        String title = "Task to Update";
        String dueDate = "2026-01-20T10:00:00";
        TaskStatus newStatus = TaskStatus.IN_PROGRESS;

        CreationDTO dto = new CreationDTO(title, null, dueDate);
        ResponseDTO created = taskService.createTask(dto);
        Long taskId = created.getId();

        // Act
        UpdateStatusDTO updateDto = new UpdateStatusDTO(newStatus);
        ResponseDTO updated = taskService.updateStatus(taskId, updateDto);

        // Assert - verify service returned updated task
        assertEquals(newStatus, updated.getStatus());

        // Assert - verify database has updated status
        Optional<Task> found = taskRepository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(newStatus, found.get().getStatus());
    }

    @Test
    void shouldDeleteTaskFromDatabase() {
        // Arrange
        String title = "Task to Delete";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO dto = new CreationDTO(title, null, dueDate);
        ResponseDTO created = taskService.createTask(dto);
        Long taskId = created.getId();

        // Verify task exists
        assertTrue(taskRepository.existsById(taskId));

        // Act
        taskService.deleteTask(taskId);

        // Assert - verify task is gone from database
        assertFalse(taskRepository.existsById(taskId));
        Optional<Task> found = taskRepository.findById(taskId);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldGetAllTasksSortedByDueDate() {
        // Arrange
        String title1 = "Task 1";
        String title2 = "Task 2";
        String title3 = "Task 3";
        String dueDate1 = "2026-01-25T10:00:00";
        String dueDate2 = "2026-01-20T10:00:00";
        String dueDate3 = "2026-01-22T10:00:00";

        CreationDTO task1 = new CreationDTO(title1, null, dueDate1);
        CreationDTO task2 = new CreationDTO(title2, null, dueDate2);
        CreationDTO task3 = new CreationDTO(title3, null, dueDate3);

        taskService.createTask(task1);
        taskService.createTask(task2);
        taskService.createTask(task3);

        // Act
        List<ResponseDTO> tasks = taskService.getAllTasks();

        // Assert - verify sorted by due date ascending
        assertTrue(tasks.size() >= 3);

        // Find our test tasks in the results
        ResponseDTO foundTask2 = tasks.stream().filter(t -> t.getTitle().equals(title2)).findFirst().orElse(null);
        ResponseDTO foundTask3 = tasks.stream().filter(t -> t.getTitle().equals(title3)).findFirst().orElse(null);
        ResponseDTO foundTask1 = tasks.stream().filter(t -> t.getTitle().equals(title1)).findFirst().orElse(null);

        assertNotNull(foundTask2);
        assertNotNull(foundTask3);
        assertNotNull(foundTask1);

        // Verify they're in the right order (earliest first)
        int indexTask2 = tasks.indexOf(foundTask2);
        int indexTask3 = tasks.indexOf(foundTask3);
        int indexTask1 = tasks.indexOf(foundTask1);

        assertTrue(indexTask2 < indexTask3);  // Jan 20 before Jan 22
        assertTrue(indexTask3 < indexTask1);  // Jan 22 before Jan 25
    }

    @Test
    void shouldFilterTasksByStatus() {
        // Arrange
        String pendingTitle = "Pending Task";
        String inProgressTitle = "In Progress Task";
        String dueDate1 = "2026-01-20T10:00:00";
        String dueDate2 = "2026-01-21T10:00:00";
        TaskStatus pendingStatus = TaskStatus.PENDING;
        TaskStatus inProgressStatus = TaskStatus.IN_PROGRESS;

        CreationDTO pendingDto = new CreationDTO(pendingTitle, null, dueDate1);
        ResponseDTO pendingTask = taskService.createTask(pendingDto);

        CreationDTO inProgressDto = new CreationDTO(inProgressTitle, null, dueDate2);
        ResponseDTO inProgressTask = taskService.createTask(inProgressDto);
        UpdateStatusDTO updateDto = new UpdateStatusDTO(inProgressStatus);
        taskService.updateStatus(inProgressTask.getId(), updateDto);

        // Act
        List<ResponseDTO> pendingTasks = taskService.getAllTasksByStatus(pendingStatus);

        // Assert - verify only PENDING tasks returned
        assertNotNull(pendingTasks);
        assertTrue(pendingTasks.stream().allMatch(t -> t.getStatus() == pendingStatus));
        assertTrue(pendingTasks.stream().anyMatch(t -> t.getTitle().equals(pendingTitle)));
        assertFalse(pendingTasks.stream().anyMatch(t -> t.getTitle().equals(inProgressTitle)));
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFoundInDatabase() {
        // Arrange
        Long nonExistentId = 99999L;

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.getTask(nonExistentId));
    }

    @Test
    void shouldSetTimestampsAutomaticallyOnCreation() {
        // Arrange
        String title = "Timestamp Test Task";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO dto = new CreationDTO(title, null, dueDate);

        // Act
        ResponseDTO created = taskService.createTask(dto);

        // Assert - verify timestamps were set
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        // Verify timestamps are recent (within last few seconds)
        LocalDateTime now = LocalDateTime.now();
        assertTrue(created.getCreatedAt().isBefore(now.plusSeconds(1)));
        assertTrue(created.getCreatedAt().isAfter(now.minusSeconds(5)));
        assertTrue(created.getUpdatedAt().isBefore(now.plusSeconds(1)));
        assertTrue(created.getUpdatedAt().isAfter(now.minusSeconds(5)));
    }

    @Test
    void shouldHandleNullDescription() {
        // Arrange
        String title = "Task Without Description";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO dto = new CreationDTO(title, null, dueDate);

        // Act
        ResponseDTO created = taskService.createTask(dto);

        // Assert
        assertNotNull(created);
        assertEquals(title, created.getTitle());
        assertNull(created.getDescription());

        // Verify in database
        Optional<Task> found = taskRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getDescription());
    }
}
