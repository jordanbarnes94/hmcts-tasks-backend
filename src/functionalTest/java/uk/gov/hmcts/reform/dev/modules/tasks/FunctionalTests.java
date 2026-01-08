package uk.gov.hmcts.reform.dev.modules.tasks;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Functional tests for Task Management API.
 * Tests the complete HTTP stack: Controller -> Service -> Repository -> Database
 * Uses RestAssured to make real HTTP requests to the running application
 *
 * <p>Test database is seeded with 20 tasks via V99__test_data.sql:
 * EXPENDABLE TASKS (can be modified/deleted by tests):
 * - ID 100: PENDING "Deletable - Pending", due 2026-01-15
 * - ID 101: IN_PROGRESS "Deletable - In Progress", due 2026-01-16
 * - ID 102: COMPLETED "Deletable - Completed", due 2026-01-17
 * - ID 103: PENDING "Modifiable - Pending", due 2026-01-18
 * - ID 104: PENDING "Modifiable - No Description", due 2026-01-19 (no description)
 *
 * PROTECTED TASKS (never modified, safe for read-only tests):
 * - IDs 105-109: PENDING tasks, due 2026-01-20 to 2026-01-24
 * - IDs 110-114: IN_PROGRESS tasks, due 2026-01-25 to 2026-01-29
 * - IDs 115-119: COMPLETED tasks, due 2026-01-30 to 2026-02-03
 * </p>
 */
@SuppressWarnings({"checkstyle:SummaryJavadoc", "checkstyle:JavadocParagraph"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FunctionalTests {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/tasks";
    }

    // ========================================
    // HAPPY PATH TESTS - CRUD Operations
    // ========================================

    /**
     * Test 1: Create task with all fields via POST /api/tasks
     * Verifies: 201 Created, correct response body structure
     */
    @Test
    void shouldCreateTaskWithAllFieldsViaHttpPost() {
        String requestBody = """
            {
                "title": "Review documentation",
                "description": "Check API docs for completeness",
                "dueDate": "2026-02-15T14:30:00"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(201)
            .body("id", notNullValue()).body("title", equalTo("Review documentation"))
            .body("description", equalTo("Check API docs for completeness")).body("status", equalTo("PENDING"))
            .body("dueDate", equalTo("2026-02-15T14:30:00")).body("createdAt", notNullValue())
            .body("updatedAt", notNullValue());
    }

    /**
     * Test 2: Create task without optional description
     */
    @Test
    void shouldCreateTaskWithoutDescription() {
        String requestBody = """
            {
                "title": "Simple task without description",
                "dueDate": "2026-02-15T14:30:00"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(201)
            .body("title", equalTo("Simple task without description")).body("description", nullValue());
    }

    /**
     * Test 3: Get all tasks via GET /api/tasks
     * Verifies: 200 OK, returns array including seeded tasks
     */
    @Test
    void shouldGetAllTasksViaHttpGet() {
        // Seed data: 20 tasks total (5 expendable: 100-104, 15 protected: 105-119)
        // Other tests may delete expendable tasks, but protected tasks remain
        given().when().get().then().statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(15)))  // At least 15 protected tasks
            .body("id", hasItems(105, 106, 107, 108, 109));  // Verify some protected IDs
    }

    /**
     * Test 4: Get single task by ID via GET /api/tasks/{id}
     */
    @Test
    void shouldGetSingleTaskById() {
        // Use protected task that's never modified
        given().when().get("/{id}", 105).then().statusCode(200).body("id", equalTo(105))
            .body("title", equalTo("Protected - Pending 1")).body("description", equalTo("Safe task for filtering"))
            .body("status", equalTo("PENDING"));
    }

    /**
     * Test 5: Filter tasks by status via GET /api/tasks?status=PENDING
     */
    @Test
    void shouldFilterTasksByPendingStatus() {
        // Protected PENDING tasks: 105-109 (5 tasks)
        given().queryParam("status", "PENDING").when().get().then().statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(5)))  // At least 5 protected PENDING tasks
            .body("status", everyItem(equalTo("PENDING")))
            .body("id", hasItems(105, 106, 107, 108, 109));  // All protected PENDING IDs
    }

    /**
     * Test 6: Filter tasks by IN_PROGRESS status
     */
    @Test
    void shouldFilterTasksByInProgressStatus() {
        // Protected IN_PROGRESS tasks: 110-114 (5 tasks)
        given().queryParam("status", "IN_PROGRESS").when().get().then().statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(5)))  // At least 5 protected IN_PROGRESS tasks
            .body("status", everyItem(equalTo("IN_PROGRESS")))
            .body("id", hasItems(110, 111, 112, 113, 114));  // All protected IN_PROGRESS IDs
    }

    /**
     * Test 7: Update task status via PATCH /api/tasks/{id}/status
     */
    @Test
    void shouldUpdateTaskStatusViaPatch() {
        // Update task 103 (expendable/modifiable) from PENDING to IN_PROGRESS
        String updateBody = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().patch("/{id}/status", 103).then().statusCode(200)
            .body("id", equalTo(103)).body("status", equalTo("IN_PROGRESS"))
            .body("title", equalTo("Modifiable - Pending"));  // Other fields unchanged
    }

    /**
     * Test 8: Delete task via DELETE /api/tasks/{id}
     */
    @Test
    void shouldDeleteTaskViaHttpDelete() {
        // Delete expendable task 102
        given().when().delete("/{id}", 102).then().statusCode(204);  // No Content

        // Verify task is gone
        given().when().get("/{id}", 102).then().statusCode(404);
    }

    /**
     * Test 9: Full CRUD workflow test
     * Create -> Read -> Update -> Delete in sequence
     */
    @Test
    void shouldPerformFullCrudWorkflow() {
        // 1. CREATE
        String createBody = """
            {
                "title": "Workflow test task",
                "description": "Testing full CRUD cycle",
                "dueDate": "2026-02-15T14:30:00"
            }
            """;

        Integer taskId =
            given().contentType(ContentType.JSON).body(createBody).post().then().statusCode(201).extract().path("id");

        // 2. READ
        given().get("/{id}", taskId).then().statusCode(200).body("title", equalTo("Workflow test task"))
            .body("status", equalTo("PENDING"));

        // 3. UPDATE
        String updateBody = """
            {
                "status": "COMPLETED"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).patch("/{id}/status", taskId).then().statusCode(200)
            .body("status", equalTo("COMPLETED"));

        // 4. DELETE
        given().delete("/{id}", taskId).then().statusCode(204);

        // 5. VERIFY DELETED
        given().get("/{id}", taskId).then().statusCode(404);
    }

    /**
     * Test 10: Verify tasks are sorted by due date (ascending)
     */
    @Test
    void shouldReturnTasksSortedByDueDate() {
        given().when().get().then().statusCode(200)
            // Seed data sorted by due date: ID 100 has earliest (2026-01-15)
            .body("[0].id", equalTo(100)).body("[0].dueDate", equalTo("2026-01-15T10:00:00"));
    }

    // ========================================
    // SAD PATH TESTS - Error Handling
    // ========================================

    /**
     * Test 11: 404 when getting non-existent task
     * Tests GlobalExceptionHandler for TaskNotFoundException
     */
    @Test
    void shouldReturn404WhenTaskNotFound() {
        given().when().get("/{id}", 99999).then().statusCode(404).body("status", equalTo(404))
            .body("error", equalTo("Not Found")).body("path", equalTo("/api/tasks/99999"));
        // Don't assert on message - it's implementation detail
    }

    /**
     * Test 12: 404 when updating non-existent task
     */
    @Test
    void shouldReturn404WhenUpdatingNonExistentTask() {
        String updateBody = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().patch("/{id}/status", 99999).then()
            .statusCode(404).body("status", equalTo(404)).body("error", equalTo("Not Found"));
        // Don't assert on message - wording may change
    }

    /**
     * Test 13: 404 when deleting non-existent task
     */
    @Test
    void shouldReturn404WhenDeletingNonExistentTask() {
        given().when().delete("/{id}", 99999).then().statusCode(404).body("status", equalTo(404))
            .body("error", equalTo("Not Found"));
        // Don't assert on message - wording may change
    }

    /**
     * Test 14: 400 when title is missing (validation error)
     * Tests GlobalExceptionHandler validation handling
     */
    @Test
    void shouldReturn400WhenTitleMissing() {
        String requestBody = """
            {
                "description": "No title provided",
                "dueDate": "2026-02-15T14:30:00"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400)).body("error", equalTo("Validation Failed"))
            .body("validationErrors.title", notNullValue());  // Title has validation error
    }

    /**
     * Test 15: 400 when due date is missing (validation error)
     */
    @Test
    void shouldReturn400WhenDueDateMissing() {
        String requestBody = """
            {
                "title": "Task without due date"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400)).body("error", equalTo("Validation Failed"))
            .body("validationErrors.dueDate", notNullValue());  // DueDate has validation error
    }

    /**
     * Test 16: 400 when title exceeds maximum length
     */
    @Test
    void shouldReturn400WhenTitleTooLong() {
        String longTitle = "a".repeat(201); // Max is 200
        String requestBody = String.format("""
            {
                "title": "%s",
                "dueDate": "2026-02-15T14:30:00"
            }
            """, longTitle);

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400)).body("validationErrors.title", notNullValue());  // Title has validation error
    }

    /**
     * Test 17: 400 when description exceeds maximum length
     */
    @Test
    void shouldReturn400WhenDescriptionTooLong() {
        String longDescription = "a".repeat(1001); // Max is 1000
        String requestBody = String.format("""
            {
                "title": "Valid title",
                "description": "%s",
                "dueDate": "2026-02-15T14:30:00"
            }
            """, longDescription);

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400))
            .body("validationErrors.description", notNullValue());  // Description has validation error
    }

    /**
     * Test 18: 400 when status value is invalid in update
     */
    @Test
    void shouldReturn400WhenInvalidStatusInUpdate() {
        String updateBody = """
            {
                "status": "INVALID_STATUS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when()
            .patch("/{id}/status", 105)  // Use protected task (doesn't matter, fails before reaching DB)
            .then().statusCode(400);
        // Just verify it's a 400 - exact error handling for enum parsing may vary
    }

    /**
     * Test 19: 400 when title is blank (empty string)
     * Tests @NotBlank validation
     */
    @Test
    void shouldReturn400WhenTitleIsBlank() {
        String requestBody = """
            {
                "title": "   ",
                "dueDate": "2026-02-15T14:30:00"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("validationErrors.title", notNullValue());  // Title has validation error
    }

    /**
     * Test 20: 400 with multiple validation errors
     * Tests that all validation errors are returned together
     */
    @Test
    void shouldReturn400WithMultipleValidationErrors() {
        String requestBody = """
            {
                "description": "Missing both title and dueDate"
            }
            """;

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400)).body("error", equalTo("Validation Failed"))
            .body("validationErrors.title", notNullValue())    // Title has validation error
            .body("validationErrors.dueDate", notNullValue()); // DueDate has validation error
    }
}
