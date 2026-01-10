# HMCTS Task Management API

A Spring Boot REST API for HMCTS caseworkers to manage their tasks, featuring pagination, search, and comprehensive filtering.

## Quick Start

### Prerequisites

- Java 21+
- Git

### Run the Application

```bash
# Clone and navigate
git clone https://github.com/jordanbarnes94/hmcts-tasks-frontend
cd hmcts-tasks-backend

# Run (downloads dependencies automatically)
./gradlew bootRun
```

**The API is now running at: http://localhost:4000**

### Access Points

| Resource       | URL                                   | Purpose              |
| -------------- | ------------------------------------- | -------------------- |
| **API Base**   | http://localhost:4000/api             | All task endpoints   |
| **Swagger UI** | http://localhost:4000/swagger-ui.html | Interactive API docs |
| **H2 Console** | http://localhost:4000/h2-console      | Database browser     |

**H2 Console Login:**

- JDBC URL: `jdbc:h2:file:./data/taskdb;MODE=PostgreSQL`
- Username: `admin`
- Password: _(leave blank)_

### Quick API Test

```bash
# Get all tasks (paginated)
curl http://localhost:4000/api/tasks

# Search tasks
curl "http://localhost:4000/api/tasks?search=review&status=PENDING"

# Get specific page
curl "http://localhost:4000/api/tasks?page=0&size=5"

# Create a task
curl -X POST http://localhost:4000/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Review case CCD-2024-001",
    "description": "Complete initial assessment",
    "dueDate": "2026-01-15T10:00:00"
  }'

# Update entire task
curl -X PUT http://localhost:4000/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-01-20T14:00:00",
    "status": "IN_PROGRESS"
  }'
```

---

## Testing

### Run Tests

```bash
# Unit tests (fast, isolated components)
./gradlew test

# Integration tests (service + database)
./gradlew integration

# Functional tests (full HTTP stack)
./gradlew functional

# Smoke tests (quick health checks)
./gradlew smoke

# All tests
./gradlew test integration functional smoke
```

### View Test Reports

After running tests, open these in your browser:

```bash
# Unit tests
build/reports/tests/test/index.html

# Integration tests
build/reports/tests/integration/index.html

# Functional tests
build/reports/tests/functional/index.html

# Code coverage
./gradlew jacocoTestReport
build/reports/jacoco/test/html/index.html
```

---

## API Endpoints

| Method   | Endpoint                 | Description                                |
| -------- | ------------------------ | ------------------------------------------ |
| `POST`   | `/api/tasks`             | Create task (auto-assigned PENDING status) |
| `GET`    | `/api/tasks`             | Get all tasks (paginated, searchable)      |
| `GET`    | `/api/tasks/{id}`        | Get single task                            |
| `PUT`    | `/api/tasks/{id}`        | Update entire task                         |
| `PATCH`  | `/api/tasks/{id}/status` | Update task status only                    |
| `DELETE` | `/api/tasks/{id}`        | Delete task                                |

### Query Parameters for GET /api/tasks

| Parameter     | Type     | Description                                     | Example                            |
| ------------- | -------- | ----------------------------------------------- | ---------------------------------- |
| `status`      | Enum     | Filter by status                                | `?status=PENDING`                  |
| `search`      | String   | Search title and description (case-insensitive) | `?search=review`                   |
| `dueDateFrom` | DateTime | Filter tasks due after this date                | `?dueDateFrom=2026-01-01T00:00:00` |
| `dueDateTo`   | DateTime | Filter tasks due before this date               | `?dueDateTo=2026-01-31T23:59:59`   |
| `page`        | Integer  | Page number (0-indexed, default: 0)             | `?page=1`                          |
| `size`        | Integer  | Items per page (default: 10)                    | `?size=20`                         |

**Combine filters:** `?status=PENDING&search=urgent&page=0&size=10`

**Available Statuses:** `PENDING`, `IN_PROGRESS`, `COMPLETED`

### Paginated Response Format

GET /api/tasks now returns a paginated response:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Review case",
      "description": "Complete assessment",
      "status": "PENDING",
      "dueDate": "2026-01-15T10:00:00",
      "createdAt": "2026-01-01T09:00:00",
      "updatedAt": "2026-01-01T09:00:00"
    }
  ],
  "totalPages": 5,
  "totalElements": 47,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

**For full API documentation:** Visit http://localhost:4000/swagger-ui.html while app is running

---

## Technology Stack

- **Java 21** – Modern LTS version
- **Spring Boot 3.5.8** – Application framework
- **H2 Database** – File-based database (PostgreSQL mode)
- **Flyway** – Database migrations
- **Spring Data JPA** – Database access
- **JPA Specifications** – Dynamic query building
- **SpringDoc OpenAPI** – API documentation
- **JUnit 5 + Mockito** – Testing

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── uk/gov/hmcts/reform/dev/
│   │       ├── Application.java              # Main entry point
│   │       └── modules/
│   │           ├── tasks/                    # Task management module
│   │           │   ├── controllers/          # REST endpoints
│   │           │   ├── services/             # Business logic
│   │           │   ├── repositories/         # Data access
│   │           │   ├── specifications/       # Dynamic query builders
│   │           │   ├── models/               # JPA entities
│   │           │   └── dtos/                 # Request/response objects
│   │           └── global/                   # Cross-cutting concerns
│   │               ├── controllers/          # Root endpoint
│   │               ├── exceptions/           # Error handling
│   │               └── dtos/                 # Shared DTOs
│   └── resources/
│       ├── application.yaml                  # Configuration
│       └── db/migration/                     # Database migrations
│           ├── V1__create_tasks_table.sql
│           ├── V2__insert_test_data.sql
│           └── V3__add_task_indexes.sql      # Performance indexes
├── test/                                     # Unit tests
├── integrationTest/                          # Integration tests
├── functionalTest/                           # End-to-end HTTP tests
└── smokeTest/                                # Deployment health checks
```

---

## Architecture Highlights

### Layered Architecture

```
Controller → Service → Repository → Database
                         ↓
                   Specifications
```

- **Controllers** - HTTP handling, validation
- **Services** - Business logic, entity↔DTO conversion
- **Repositories** - Database access (Spring Data JPA + Specifications)
- **Specifications** - Dynamic query building for filtering/search
- **Database** - H2 in PostgreSQL mode (file-based)

### Key Design Decisions

1. **Database-First with Flyway** - Schema managed via SQL migrations, not JPA
2. **DTO Pattern** - Separate request/response objects for security
3. **JPA Specifications** - Type-safe dynamic queries for flexible filtering
4. **Automatic Timestamps** - `@PrePersist` / `@PreUpdate` callbacks
5. **Enum for Status** - Type-safe task statuses
6. **Global Exception Handling** - Consistent error responses
7. **Database Indexes** - Optimized for common query patterns
8. **Four-Tier Testing** - Unit, Integration, Functional, Smoke tests

### Why Specifications?

Instead of creating dozens of repository methods for every filter combination:

```pseudocode
// BAD: Method explosion
findByStatus(...)
findByStatusAndDueDate(...)
findByStatusAndDueDateAndTitle(...)
// ... 20+ methods
```

We use **JPA Specifications** for dynamic, composable queries:

```java
// GOOD: One flexible method
Specification<Task> spec = TaskSpecifications.withFilters(status, search, dateFrom, dateTo);
Page<Task> tasks = repository.findAll(spec, pageable);
```

This approach is:

- **Type-safe** - Compile-time checking
- **Composable** - Mix and match filters
- **Indexable** - Generates optimal SQL with WHERE clauses only for non-null parameters

---

## Database

### Schema

The `tasks` table:

- `id` - Auto-incrementing primary key
- `title` - Task title (required, max 200 chars)
- `description` - Optional details (max 1000 chars)
- `status` - PENDING, IN_PROGRESS, or COMPLETED
- `due_date` - Task deadline
- `created_at` - Auto-set on creation
- `updated_at` - Auto-updated on modification

### Indexes

Performance indexes for common queries:

- `idx_tasks_status` - Status filtering
- `idx_tasks_due_date` - Date range queries and sorting
- `idx_tasks_status_due_date` - Combined status + date filtering

### Migrations

Schema is version-controlled with Flyway:

- `V1__create_tasks_table.sql` - Initial schema
- `V2__insert_test_data.sql` - Sample data for development
- `V3__add_task_indexes.sql` - Performance indexes

### Database Files

Located in `./data/taskdb.mv.db` (persists between runs)

---

## Development Tips

### Hot Reload

Spring Boot DevTools is included - code changes reload automatically.

### Viewing SQL Queries

Already enabled in development:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

### Clean Database

```bash
# Delete database file to start fresh
rm data/taskdb.mv.db

# Restart app - Flyway will recreate from migrations
./gradlew bootRun
```

### Code Quality

```bash
# Run checkstyle
./gradlew checkstyleMain checkstyleTest

# Check for dependency vulnerabilities
./gradlew dependencyCheckAnalyze
```

---

## Production Considerations

For production deployment, consider:

1. **Replace H2 with PostgreSQL** - Just change JDBC URL in `application.yaml`
2. **Add Spring Security** - JWT authentication, role-based access
3. **Tune Pagination Defaults** - Adjust page size limits for performance
4. **Add Caching** - Redis for frequently accessed tasks
5. **Full-Text Search** - PostgreSQL `to_tsvector` with GIN indexes for better text search performance
6. **Structured Logging** - JSON format for log aggregation
7. **Monitoring** - Prometheus metrics, distributed tracing
8. **Timezone Handling** - Use `Instant` instead of `LocalDateTime` for multi-timezone support

---

## Built With

Spring Boot best practices including:

- Constructor injection for dependencies
- Database-first schema management
- Dynamic query building with JPA Specifications
- Comprehensive test coverage
- Global exception handling
- API documentation with Swagger
- Performance-optimized database indexes

---

## License

This is a technical test project for HMCTS recruitment.
