package uk.gov.hmcts.reform.dev.modules.tasks.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.CreationDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.ResponseDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.dtos.UpdateStatusDTO;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;
import uk.gov.hmcts.reform.dev.modules.tasks.services.TaskService;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ResponseDTO> createTask(@Valid @RequestBody CreationDTO request) {
        ResponseDTO response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResponseDTO>> getAllTasks(
        @RequestParam(required = false) TaskStatus status
    ) {
        List<ResponseDTO> tasks = (status != null)
            ? taskService.getAllTasksByStatus(status)
            : taskService.getAllTasks();

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getTask(@PathVariable Long id) {
        ResponseDTO response = taskService.getTask(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseDTO> updateTaskStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusDTO request
    ) {
        ResponseDTO response = taskService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
