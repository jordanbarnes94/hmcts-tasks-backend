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
 * PROTECTED TASKS (never modified, safe for read-only tests):
 * - IDs 999001-999005: PENDING tasks, due 2026-01-20 to 2026-01-24
 * - IDs 999006-999010: IN_PROGRESS tasks, due 2026-01-25 to 2026-01-29
 * - IDs 999011-999015: COMPLETED tasks, due 2026-01-30 to 2026-02-03
 *
 * <p>
 * EXPENDABLE TASKS (can be modified/deleted by tests):
 * - ID 999016: PENDING "Deletable - Pending", due 2026-01-15
 * - ID 999017: IN_PROGRESS "Deletable - In Progress", due 2026-01-16
 * - ID 999018: COMPLETED "Deletable - Completed", due 2026-01-17
 * - ID 999019: PENDING "Modifiable - Pending", due 2026-01-18
 * - ID 999020: PENDING "Modifiable - No Description", due 2026-01-19 (no description)
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
        // Seed data: 20 tasks total (15 protected: 999001-999015, 5 expendable: 999016-999020)
        // Other tests may delete expendable tasks, but protected tasks remain
        given().when().get().then().statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(15))  // Check total, not content size
            .body("content.id", hasItems(999001, 999002, 999003, 999004, 999005));
    }

    /**
     * Test 4: Get single task by ID via GET /api/tasks/{id}
     */
    @Test
    void shouldGetSingleTaskById() {
        // Use protected task that's never modified
        given().when().get("/{id}", 999001).then().statusCode(200).body("id", equalTo(999001))
            .body("title", equalTo("Protected - Pending 1")).body("description", equalTo("Safe task for filtering"))
            .body("status", equalTo("PENDING"));
    }

    /**
     * Test 5: Filter tasks by status via GET /api/tasks?status=PENDING
     */
    @Test
    void shouldFilterTasksByPendingStatus() {
        // Protected PENDING tasks: 999001-999005 (5 tasks)
        given().queryParam("status", "PENDING").when().get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(5)))  // At least 5 protected PENDING tasks
            .body("content.status", everyItem(equalTo("PENDING")))
            .body("content.id", hasItems(999001, 999002, 999003, 999004, 999005));  // All protected PENDING IDs
    }

    /**
     * Test 6: Filter tasks by IN_PROGRESS status
     */
    @Test
    void shouldFilterTasksByInProgressStatus() {
        // Protected IN_PROGRESS tasks: 999006-999010 (5 tasks)
        given().queryParam("status", "IN_PROGRESS").when().get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(5)))  // At least 5 protected IN_PROGRESS tasks
            .body("content.status", everyItem(equalTo("IN_PROGRESS")))
            .body("content.id", hasItems(999006, 999007, 999008, 999009, 999010));  // All protected IN_PROGRESS IDs
    }

    /**
     * Test 7: Update task status via PATCH /api/tasks/{id}/status
     */
    @Test
    void shouldUpdateTaskStatusViaPatch() {
        // Update task 999019 (expendable/modifiable) from PENDING to IN_PROGRESS
        String updateBody = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().patch("/{id}/status", 999019).then().statusCode(200)
            .body("id", equalTo(999019)).body("status", equalTo("IN_PROGRESS"))
            .body("title", equalTo("Modifiable - Pending"));  // Other fields unchanged
    }

    /**
     * Test 8: Delete task via DELETE /api/tasks/{id}
     */
    @Test
    void shouldDeleteTaskViaHttpDelete() {
        // Delete expendable task 999018
        given().when().delete("/{id}", 999018).then().statusCode(204);  // No Content

        // Verify task is gone
        given().when().get("/{id}", 999018).then().statusCode(404);
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
            // Seed data sorted by due date: ID 999016 has earliest (2026-01-15)
            .body("content[0].id", equalTo(999016))
            .body("content[0].dueDate", equalTo("2026-01-15T10:00:00"));
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
        String requestBody = String.format(
            """
                {
                    "title": "%s",
                    "dueDate": "2026-02-15T14:30:00"
                }
                """, longTitle
        );

        given().contentType(ContentType.JSON).body(requestBody).when().post().then().statusCode(400)
            .body("status", equalTo(400)).body("validationErrors.title", notNullValue());  // Title has validation error
    }

    /**
     * Test 17: 400 when description exceeds maximum length
     */
    @Test
    void shouldReturn400WhenDescriptionTooLong() {
        String longDescription = "a".repeat(1001); // Max is 1000
        String requestBody = String.format(
            """
                {
                    "title": "Valid title",
                    "description": "%s",
                    "dueDate": "2026-02-15T14:30:00"
                }
                """, longDescription
        );

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
            .patch("/{id}/status", 999001)  // Use protected task (doesn't matter, fails before reaching DB)
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

    /**
     * Test: Update task via PUT /api/tasks/{id}
     * Verifies: 200 OK, all fields updated correctly
     */
    @Test
    void shouldUpdateTaskViaHttpPut() {
        // Use expendable task 999020 (Modifiable - No Description)
        String updateBody = """
            {
                "title": "Fully Updated Task",
                "description": "Now has a description",
                "dueDate": "2026-03-15T16:30:00",
                "status": "IN_PROGRESS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999020).then().statusCode(200)
            .body("id", equalTo(999020)).body("title", equalTo("Fully Updated Task"))
            .body("description", equalTo("Now has a description")).body("dueDate", equalTo("2026-03-15T16:30:00"))
            .body("status", equalTo("IN_PROGRESS"));
    }

    /**
     * Test: Update status via PUT (not just PATCH)
     */
    @Test
    void shouldUpdateStatusViaPut() {
        // Use expendable task 999019 (Modifiable - Pending)
        // Keep other fields the same, just change status
        String updateBody = """
            {
                "title": "Modifiable - Pending",
                "description": "Used in update test",
                "dueDate": "2026-01-18T10:00:00",
                "status": "COMPLETED"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999019).then().statusCode(200)
            .body("id", equalTo(999019)).body("status", equalTo("COMPLETED"));

        // Verify it persisted
        given().when().get("/{id}", 999019).then().statusCode(200).body("status", equalTo("COMPLETED"));
    }

    /**
     * Test: 404 when updating non-existent task via PUT
     */
    @Test
    void shouldReturn404WhenUpdatingNonExistentTaskViaPut() {
        String updateBody = """
            {
                "title": "Doesn't matter",
                "dueDate": "2026-02-15T14:30:00",
                "status": "PENDING"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 99999).then().statusCode(404)
            .body("status", equalTo(404)).body("error", equalTo("Not Found"));
    }

    /**
     * Test: 400 when title is missing in PUT
     */
    @Test
    void shouldReturn400WhenPutTitleMissing() {
        String updateBody = """
            {
                "description": "Missing title",
                "dueDate": "2026-02-15T14:30:00",
                "status": "PENDING"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999001).then().statusCode(400)
            .body("status", equalTo(400)).body("error", equalTo("Validation Failed"))
            .body("validationErrors.title", notNullValue());
    }

    /**
     * Test: 400 when dueDate is missing in PUT
     */
    @Test
    void shouldReturn400WhenPutDueDateMissing() {
        String updateBody = """
            {
                "title": "Missing due date",
                "status": "PENDING"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999001).then().statusCode(400)
            .body("status", equalTo(400)).body("error", equalTo("Validation Failed"))
            .body("validationErrors.dueDate", notNullValue());
    }

    /**
     * Test: 400 when title exceeds maximum length in PUT
     */
    @Test
    void shouldReturn400WhenPutTitleTooLong() {
        String longTitle = "a".repeat(201); // Max is 200
        String updateBody = String.format(
            """
                {
                    "title": "%s",
                    "dueDate": "2026-02-15T14:30:00",
                    "status": "PENDING"
                }
                """, longTitle
        );

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999001).then().statusCode(400)
            .body("status", equalTo(400)).body("validationErrors.title", notNullValue());
    }

    /**
     * Test: 400 when status value is invalid in PUT
     */
    @Test
    void shouldReturn400WhenPutStatusInvalid() {
        String updateBody = """
            {
                "title": "Valid title",
                "dueDate": "2026-02-15T14:30:00",
                "status": "INVALID_STATUS"
            }
            """;

        given().contentType(ContentType.JSON).body(updateBody).when().put("/{id}", 999001).then().statusCode(400);
    }

    /**
     * Test: Paginate tasks via HTTP
     * Verifies: Page structure, metadata (totalPages, number, size, etc.)
     */
    @Test
    void shouldPaginateTasksViaHttp() {
        // Using protected tasks (999001-999015, 15 tasks minimum)
        // Request page 0, size 5
        given().queryParam("page", 0).queryParam("size", 5).when().get().then().statusCode(200)
            .body("content", hasSize(5))  // Should have 5 items
            .body("size", equalTo(5))  // Page size
            .body("number", equalTo(0))  // Current page (0-indexed)
            .body("totalPages", greaterThanOrEqualTo(3))  // At least 3 pages (15 tasks / 5 per page)
            .body("totalElements", greaterThanOrEqualTo(15))  // At least 15 tasks
            .body("first", equalTo(true))  // Is first page
            .body("last", equalTo(false));  // Not last page
    }

    /**
     * Test: Request second page
     */
    @Test
    void shouldGetSecondPageViaHttp() {
        given().queryParam("page", 1).queryParam("size", 5).when().get().then().statusCode(200)
            .body("number", equalTo(1))  // Second page
            .body("first", equalTo(false))  // Not first page
            .body("content", hasSize(greaterThanOrEqualTo(1)));  // Has content
    }

    /**
     * Test: Search tasks by text via HTTP
     */
    @Test
    void shouldSearchTasksByTextViaHttp() {
        // Protected task 999001 has title "Protected - Pending 1"
        given().queryParam("search", "Protected").when().get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(1))).body("content.title", hasItems("Protected - Pending 1"));
    }

    /**
     * Test: Search is case-insensitive
     */
    @Test
    void shouldSearchCaseInsensitiveViaHttp() {
        given().queryParam("search", "protected")  // lowercase
            .when().get().then().statusCode(200).body("content", hasSize(greaterThanOrEqualTo(1)))
            .body("content.title", hasItems("Protected - Pending 1"));
    }

    /**
     * Test: Filter by date range via HTTP
     */
    @Test
    void shouldFilterByDateRangeViaHttp() {
        // Protected tasks 999001-999005 are due 2026-01-20 to 2026-01-24
        given().queryParam("dueDateFrom", "2026-01-20T00:00:00").queryParam("dueDateTo", "2026-01-24T23:59:59").when()
            .get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(5)))  // At least 5 protected PENDING tasks
            .body("content.id", hasItems(999001, 999002, 999003, 999004, 999005));
    }

    /**
     * Test: Filter by status and date range
     */
    @Test
    void shouldCombineStatusAndDateRangeViaHttp() {
        // Protected PENDING tasks 999001-999005 are due 2026-01-20 to 2026-01-24
        given().queryParam("status", "PENDING").queryParam("dueDateFrom", "2026-01-20T00:00:00")
            .queryParam("dueDateTo", "2026-01-24T23:59:59").when().get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(5))).body("content.status", everyItem(equalTo("PENDING")))
            .body("content.id", hasItems(999001, 999002, 999003, 999004, 999005));
    }

    /**
     * Test: Combine all filters (status, search, date range, pagination)
     */
    @Test
    void shouldCombineAllFiltersViaHttp() {
        // Protected PENDING tasks 999001-999005 have "Protected" in title, due 2026-01-20 to 2026-01-24
        given().queryParam("status", "PENDING").queryParam("search", "Protected")
            .queryParam("dueDateFrom", "2026-01-20T00:00:00").queryParam("dueDateTo", "2026-01-24T23:59:59")
            .queryParam("page", 0).queryParam("size", 3).when().get().then().statusCode(200)
            .body("content", hasSize(3))  // Page size 3
            .body("content.status", everyItem(equalTo("PENDING"))).body("size", equalTo(3)).body("number", equalTo(0));
    }

    /**
     * Test: Default pagination when not specified
     */
    @Test
    void shouldUseDefaultPaginationWhenNotSpecified() {
        // Should return first page with default size
        given().when().get().then().statusCode(200).body("number", equalTo(0))  // First page
            .body("size", equalTo(10))  // Default size
            .body("content", hasSize(greaterThanOrEqualTo(1)));
    }

    /**
     * Test: Empty results when no matches
     */
    @Test
    void shouldReturnEmptyPageWhenNoMatchesViaHttp() {
        given().queryParam("search", "nonexistentsearchterm12345").when().get().then().statusCode(200)
            .body("content", hasSize(0)).body("totalElements", equalTo(0)).body("totalPages", equalTo(0))
            .body("empty", equalTo(true));
    }

    /**
     * Test: Filter by due date from only (no upper bound)
     */
    @Test
    void shouldFilterByDueDateFromOnlyViaHttp() {
        // Tasks due on or after 2026-01-25 (IN_PROGRESS tasks 999006-999010)
        given().queryParam("dueDateFrom", "2026-01-25T00:00:00").when().get().then().statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(5))).body("content.id", hasItems(999006, 999007, 999008, 999009, 999010));
    }

    /**
     * Test: Filter by due date to only (no lower bound)
     */
    @Test
    void shouldFilterByDueDateToOnlyViaHttp() {
        // Includes protected tasks 999001-999005 and expendable tasks
        given().queryParam("dueDateTo", "2026-01-24T23:59:59")
            .when().get()
            .then().statusCode(200)
            .body("content.id", hasItems(999001, 999002, 999003, 999004, 999005));
    }

    /**
     * Test: Pagination preserves filters
     */
    @Test
    void shouldPreserveFiltersAcrossPagesViaHttp() {
        // Get first page with status filter
        Integer totalElements =
            given().queryParam("status", "PENDING").queryParam("page", 0).queryParam("size", 3).when().get().then()
                .statusCode(200).body("content.status", everyItem(equalTo("PENDING"))).extract().path("totalElements");

        // Get second page with same filter
        if (totalElements > 3) {  // Only test if there's a second page
            given().queryParam("status", "PENDING").queryParam("page", 1).queryParam("size", 3).when().get().then()
                .statusCode(200).body("content.status", everyItem(equalTo("PENDING")));
        }
    }
}
