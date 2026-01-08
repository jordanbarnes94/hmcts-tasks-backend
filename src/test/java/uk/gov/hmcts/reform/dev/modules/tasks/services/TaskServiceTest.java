package uk.gov.hmcts.reform.dev.modules.tasks.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateStatusDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.modules.tasks.models.Task;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;
import uk.gov.hmcts.reform.dev.modules.tasks.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldCreateTask() {
        // Arrange - Test data
        String title = "Review case";
        String description = "Important case review";
        String dueDate = "2026-01-15T10:00:00";

        // Mock repository response
        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle(title);
        savedTask.setDescription(description);
        savedTask.setStatus(TaskStatus.PENDING);
        savedTask.setDueDate(LocalDateTime.parse(dueDate));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Create DTO with same data
        CreationDTO dto = new CreationDTO(title, description, dueDate);

        // Act
        ResponseDTO result = taskService.createTask(dto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertEquals(LocalDateTime.parse(dueDate), result.getDueDate());

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldGetTask_WhenExists() {
        // Arrange - Test data
        Long taskId = 1L;
        String title = "Review case";
        String description = "Important case review";

        // Mock repository response
        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setTitle(title);
        existingTask.setDescription(description);
        existingTask.setStatus(TaskStatus.PENDING);
        existingTask.setDueDate(LocalDateTime.parse("2026-01-15T10:00:00"));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Act
        ResponseDTO result = taskService.getTask(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(TaskStatus.PENDING, result.getStatus());

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    void shouldGetTask_WhenNotExists_ThrowsException() {
        // Arrange
        Long taskId = 999L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.getTask(taskId));

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    void shouldGetAllTasks() {
        // Arrange - Test data
        Task task1 = new Task();
        task1.setId(1L);
        task1.setTitle("Task 1");
        task1.setStatus(TaskStatus.PENDING);
        task1.setDueDate(LocalDateTime.parse("2026-01-15T10:00:00"));

        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setDueDate(LocalDateTime.parse("2026-01-16T10:00:00"));

        List<Task> tasks = Arrays.asList(task1, task2);
        when(taskRepository.findAllByOrderByDueDateAsc()).thenReturn(tasks);

        // Act
        List<ResponseDTO> result = taskService.getAllTasks();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());

        verify(taskRepository, times(1)).findAllByOrderByDueDateAsc();
    }

    @Test
    void shouldGetAllTasksByStatus() {
        // Arrange - Test data
        TaskStatus status = TaskStatus.PENDING;

        Task task1 = new Task();
        task1.setId(1L);
        task1.setTitle("Pending Task 1");
        task1.setStatus(status);
        task1.setDueDate(LocalDateTime.parse("2026-01-15T10:00:00"));

        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Pending Task 2");
        task2.setStatus(status);
        task2.setDueDate(LocalDateTime.parse("2026-01-16T10:00:00"));

        List<Task> tasks = Arrays.asList(task1, task2);
        when(taskRepository.findAllByStatusOrderByDueDateAsc(status)).thenReturn(tasks);

        // Act
        List<ResponseDTO> result = taskService.getAllTasksByStatus(status);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Pending Task 1", result.get(0).getTitle());
        assertEquals("Pending Task 2", result.get(1).getTitle());
        assertEquals(status, result.get(0).getStatus());
        assertEquals(status, result.get(1).getStatus());

        verify(taskRepository, times(1)).findAllByStatusOrderByDueDateAsc(status);
    }

    @Test
    void shouldUpdateStatus_WhenExists() {
        // Arrange - Test data
        Long taskId = 1L;

        // Mock existing task
        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setTitle("Review case");
        existingTask.setStatus(TaskStatus.PENDING);
        existingTask.setDueDate(LocalDateTime.parse("2026-01-15T10:00:00"));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Mock saved task with updated status
        TaskStatus newStatus = TaskStatus.IN_PROGRESS;
        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setTitle("Review case");
        updatedTask.setStatus(newStatus);
        updatedTask.setDueDate(LocalDateTime.parse("2026-01-15T10:00:00"));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        UpdateStatusDTO dto = new UpdateStatusDTO(newStatus);

        // Act
        ResponseDTO result = taskService.updateStatus(taskId, dto);

        // Assert
        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertEquals(newStatus, result.getStatus());

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldUpdateStatus_WhenNotExists_ThrowsException() {
        // Arrange
        Long taskId = 999L;
        TaskStatus newStatus = TaskStatus.IN_PROGRESS;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        UpdateStatusDTO dto = new UpdateStatusDTO(newStatus);

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.updateStatus(taskId, dto));

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldDeleteTask_WhenExists() {
        // Arrange
        Long taskId = 1L;
        when(taskRepository.existsById(taskId)).thenReturn(true);

        // Act
        taskService.deleteTask(taskId);

        // Assert
        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void shouldDeleteTask_WhenNotExists_ThrowsException() {
        // Arrange
        Long taskId = 999L;
        when(taskRepository.existsById(taskId)).thenReturn(false);

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(taskId));

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, never()).deleteById(taskId);
    }
}
