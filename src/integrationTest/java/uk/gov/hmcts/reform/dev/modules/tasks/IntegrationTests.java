package uk.gov.hmcts.reform.dev.modules.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateDTO;
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

        // Act - Create pageable with large page size to get all tasks
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> tasksPage = taskService.getAllTasks(null, null, null, null, pageable);

        // Assert - verify sorted by due date ascending
        List<ResponseDTO> tasks = tasksPage.getContent();  // Extract list from Page
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

        // Act - Create pageable with large page size to get all tasks
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> tasksPage = taskService.getAllTasks(TaskStatus.PENDING, null, null, null, pageable);

        List<ResponseDTO> pendingTasks = tasksPage.getContent();  // Extract list from Page

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

    @Test
    void shouldUpdateTaskAndPersistToDatabase() {
        // Arrange - Create initial task
        String originalTitle = "Original Task";
        String originalDescription = "Original description";
        String originalDueDate = "2026-01-20T10:00:00";
        CreationDTO createDto = new CreationDTO(originalTitle, originalDescription, originalDueDate);
        ResponseDTO created = taskService.createTask(createDto);
        Long taskId = created.getId();

        // Prepare update data
        String newTitle = "Updated Task Title";
        String newDescription = "Updated description";
        String newDueDate = "2026-02-25T14:00:00";
        TaskStatus newStatus = TaskStatus.COMPLETED;

        // Act - Update the task
        UpdateDTO updateDto = new UpdateDTO(newTitle, newDescription, newDueDate, newStatus);
        ResponseDTO updated = taskService.updateTask(taskId, updateDto);

        // Assert - Verify service response
        assertEquals(taskId, updated.getId());
        assertEquals(newTitle, updated.getTitle());
        assertEquals(newDescription, updated.getDescription());
        assertEquals(newStatus, updated.getStatus());
        assertEquals(LocalDateTime.parse(newDueDate), updated.getDueDate());

        // Assert - Verify database has updated values
        Optional<Task> found = taskRepository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(newTitle, found.get().getTitle());
        assertEquals(newDescription, found.get().getDescription());
        assertEquals(newStatus, found.get().getStatus());
        assertEquals(LocalDateTime.parse(newDueDate), found.get().getDueDate());
    }

    @Test
    void shouldUpdateTaskWithNullDescription() {
        // Arrange - Create task with description
        String title = "Task with description";
        String description = "This will be removed";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO createDto = new CreationDTO(title, description, dueDate);
        ResponseDTO created = taskService.createTask(createDto);
        Long taskId = created.getId();

        // Verify description exists initially
        Optional<Task> initial = taskRepository.findById(taskId);
        assertTrue(initial.isPresent());
        assertNotNull(initial.get().getDescription());

        // Act - Update with null description
        UpdateDTO updateDto = new UpdateDTO(title, null, dueDate, TaskStatus.PENDING);
        ResponseDTO updated = taskService.updateTask(taskId, updateDto);

        // Assert - Verify description is null in response
        assertNull(updated.getDescription());

        // Assert - Verify description is null in database
        Optional<Task> found = taskRepository.findById(taskId);
        assertTrue(found.isPresent());
        assertNull(found.get().getDescription());
    }

    @Test
    void shouldPreserveCreatedAtOnUpdate() {
        // Arrange - Create task
        String title = "Task to update";
        String dueDate = "2026-01-20T10:00:00";
        CreationDTO createDto = new CreationDTO(title, null, dueDate);
        ResponseDTO created = taskService.createTask(createDto);
        Long taskId = created.getId();
        LocalDateTime originalCreatedAt = created.getCreatedAt();

        // Act - Update the task
        String newTitle = "Updated title";
        UpdateDTO updateDto = new UpdateDTO(newTitle, null, dueDate, TaskStatus.IN_PROGRESS);
        ResponseDTO updated = taskService.updateTask(taskId, updateDto);

        // Assert - Verify createdAt unchanged in response
        assertEquals(originalCreatedAt, updated.getCreatedAt());

        // Assert - Verify createdAt unchanged in database
        Optional<Task> found = taskRepository.findById(taskId);
        assertTrue(found.isPresent());
        assertEquals(originalCreatedAt, found.get().getCreatedAt());
    }

    @Test
    void shouldSearchTasksByTitleText() {
        // Arrange - Create tasks with different titles
        String searchTerm = "review";
        CreationDTO task1 = new CreationDTO("Please review the document", null, "2026-01-20T10:00:00");
        CreationDTO task2 = new CreationDTO("Submit report", null, "2026-01-21T10:00:00");
        CreationDTO task3 = new CreationDTO("Review meeting notes", null, "2026-01-22T10:00:00");

        taskService.createTask(task1);
        taskService.createTask(task2);
        taskService.createTask(task3);

        // Act - Search for "review" in title
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results = taskService.getAllTasks(null, searchTerm, null, null, pageable);

        // Assert - Only tasks with "review" in title should be returned
        List<ResponseDTO> tasks = results.getContent();
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().contains("review")));
        assertFalse(tasks.stream().anyMatch(t -> t.getTitle().equals("Submit report")));
    }

    @Test
    void shouldSearchTasksByDescriptionText() {
        // Arrange - Create tasks where search term only appears in description
        String searchTerm = "urgent";
        CreationDTO task1 = new CreationDTO("Task A", "This is urgent work", "2026-01-20T10:00:00");
        CreationDTO task2 = new CreationDTO("Task B", "Normal priority", "2026-01-21T10:00:00");
        CreationDTO task3 = new CreationDTO("Task C", "Urgent action required", "2026-01-22T10:00:00");

        taskService.createTask(task1);
        taskService.createTask(task2);
        taskService.createTask(task3);

        // Act - Search for "urgent" (appears only in descriptions)
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results = taskService.getAllTasks(null, searchTerm, null, null, pageable);

        // Assert - Only tasks with "urgent" in description should be returned
        List<ResponseDTO> tasks = results.getContent();
        assertTrue(tasks.stream().anyMatch(t -> "Task A".equals(t.getTitle())));
        assertTrue(tasks.stream().anyMatch(t -> "Task C".equals(t.getTitle())));
        assertFalse(tasks.stream().anyMatch(t -> "Task B".equals(t.getTitle())));
    }

    @Test
    void shouldFilterByDueDateRange() {
        // Arrange - Create tasks with different due dates
        CreationDTO task1 = new CreationDTO("Early task", null, "2026-01-15T10:00:00");
        CreationDTO task2 = new CreationDTO("Mid task", null, "2026-01-25T10:00:00");
        CreationDTO task3 = new CreationDTO("Late task", null, "2026-02-05T10:00:00");

        taskService.createTask(task1);
        taskService.createTask(task2);
        taskService.createTask(task3);

        // Act - Filter by date range (Jan 20 to Jan 31)
        LocalDateTime dueDateFrom = LocalDateTime.parse("2026-01-20T00:00:00");
        LocalDateTime dueDateTo = LocalDateTime.parse("2026-01-31T23:59:59");
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results = taskService.getAllTasks(null, null, dueDateFrom, dueDateTo, pageable);

        // Assert - Only task with due date in range should be returned
        List<ResponseDTO> tasks = results.getContent();
        assertTrue(tasks.stream().anyMatch(t -> "Mid task".equals(t.getTitle())));
        assertFalse(tasks.stream().anyMatch(t -> "Early task".equals(t.getTitle())));
        assertFalse(tasks.stream().anyMatch(t -> "Late task".equals(t.getTitle())));
    }

    @Test
    void shouldCombineMultipleFilters() {
        // Arrange - Create tasks with various attributes
        CreationDTO task1 = new CreationDTO("Review pending item", "Needs attention", "2026-01-25T10:00:00");
        CreationDTO task2 = new CreationDTO("Review completed item", "Already done", "2026-01-26T10:00:00");
        CreationDTO task3 = new CreationDTO("Submit pending item", "Needs attention", "2026-01-27T10:00:00");

        ResponseDTO created1 = taskService.createTask(task1);
        ResponseDTO created2 = taskService.createTask(task2);
        taskService.createTask(task3);

        // Update task2 to COMPLETED status
        UpdateDTO updateDto =
            new UpdateDTO("Review completed item", "Already done", "2026-01-26T10:00:00", TaskStatus.COMPLETED);
        taskService.updateTask(created2.getId(), updateDto);

        // Act - Combine filters: status=PENDING, search="review", date range
        LocalDateTime dueDateFrom = LocalDateTime.parse("2026-01-20T00:00:00");
        LocalDateTime dueDateTo = LocalDateTime.parse("2026-01-31T23:59:59");
        Pageable pageable = PageRequest.of(0, 100, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results =
            taskService.getAllTasks(TaskStatus.PENDING, "review", dueDateFrom, dueDateTo, pageable);

        // Assert - Only task1 matches all criteria
        List<ResponseDTO> tasks = results.getContent();
        assertEquals(
            1,
            tasks.stream().filter(t -> t.getTitle().contains("Review") && t.getStatus() == TaskStatus.PENDING).count()
        );
        assertTrue(tasks.stream().anyMatch(t -> "Review pending item".equals(t.getTitle())));
        assertFalse(tasks.stream().anyMatch(t -> "Review completed item".equals(t.getTitle()))); // wrong status
        assertFalse(tasks.stream().anyMatch(t -> "Submit pending item".equals(t.getTitle()))); // no "review"
    }

    @Test
    void shouldReturnPaginatedResults() {
        // Arrange - Create 25 tasks
        for (int i = 1; i <= 25; i++) {
            String title = "Task " + i;
            String dueDate = String.format("2026-01-%02dT10:00:00", i);
            CreationDTO task = new CreationDTO(title, null, dueDate);
            taskService.createTask(task);
        }

        // Act - Request page 1 (second page) with size 10
        Pageable pageable = PageRequest.of(1, 10, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results = taskService.getAllTasks(null, null, null, null, pageable);

        // Assert - Verify pagination metadata
        assertEquals(10, results.getSize()); // page size
        assertEquals(1, results.getNumber()); // current page (0-indexed, so page 1 is second page)
        assertTrue(results.getTotalElements() >= 25); // at least 25 tasks
        assertTrue(results.getTotalPages() >= 3); // at least 3 pages (25 tasks / 10 per page)
        assertFalse(results.isFirst()); // not first page
        assertFalse(results.isLast()); // not last page (assuming we have ≥25 tasks)

        // Verify content
        List<ResponseDTO> tasks = results.getContent();
        assertTrue(tasks.size() <= 10); // should not exceed page size
    }

    @Test
    void shouldReturnEmptyPageWhenNoMatches() {
        // Arrange - Create some tasks
        CreationDTO task1 = new CreationDTO("Task A", null, "2026-01-20T10:00:00");
        taskService.createTask(task1);

        // Act - Search for something that doesn't exist
        Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").ascending());
        Page<ResponseDTO> results = taskService.getAllTasks(null, "nonexistent search term", null, null, pageable);

        // Assert - Empty page with correct metadata
        assertEquals(0, results.getContent().size());
        assertEquals(0, results.getTotalElements());
        assertEquals(0, results.getTotalPages());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleCaseInsensitiveSearch() {
        // Arrange - Create task with mixed case
        CreationDTO task1 = new CreationDTO("Review Document", "Important REVIEW needed", "2026-01-20T10:00:00");
        taskService.createTask(task1);

        // Act - Search with different cases
        Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").ascending());
        Page<ResponseDTO> resultsLower = taskService.getAllTasks(null, "review", null, null, pageable);
        Page<ResponseDTO> resultsUpper = taskService.getAllTasks(null, "REVIEW", null, null, pageable);
        Page<ResponseDTO> resultsMixed = taskService.getAllTasks(null, "ReViEw", null, null, pageable);

        // Assert - All should find the task
        assertTrue(resultsLower.getContent().stream().anyMatch(t -> t.getTitle().contains("Review")));
        assertTrue(resultsUpper.getContent().stream().anyMatch(t -> t.getTitle().contains("Review")));
        assertTrue(resultsMixed.getContent().stream().anyMatch(t -> t.getTitle().contains("Review")));
    }
}
