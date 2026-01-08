# HMCTS Task Management API

A Spring Boot REST API for HMCTS caseworkers to manage their tasks.

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
- Password: *(leave blank)*

### Quick API Test

```bash
# Get all tasks
curl http://localhost:4000/api/tasks

# Create a task
curl -X POST http://localhost:4000/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Review case CCD-2024-001",
    "description": "Complete initial assessment",
    "dueDate": "2026-01-15T10:00:00"
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

| Method   | Endpoint                    | Description                                |
| -------- | --------------------------- | ------------------------------------------ |
| `POST`   | `/api/tasks`                | Create task (auto-assigned PENDING status) |
| `GET`    | `/api/tasks`                | Get all tasks (sorted by due date)         |
| `GET`    | `/api/tasks?status=PENDING` | Filter tasks by status                     |
| `GET`    | `/api/tasks/{id}`           | Get single task                            |
| `PATCH`  | `/api/tasks/{id}/status`    | Update task status                         |
| `DELETE` | `/api/tasks/{id}`           | Delete task                                |

**Available Statuses:** `PENDING`, `IN_PROGRESS`, `COMPLETED`

**For full API documentation:** Visit http://localhost:4000/swagger-ui.html while app is running

---

## Technology Stack

- **Java 21** – Modern LTS version
- **Spring Boot 3.5.8** – Application framework
- **H2 Database** – File-based database (PostgreSQL mode)
- **Flyway** – Database migrations
- **Spring Data JPA** – Database access
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
│           └── V2__insert_test_data.sql
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
```

- **Controllers** - HTTP handling, validation
- **Services** - Business logic, entity↔DTO conversion
- **Repositories** - Database access (Spring Data JPA)
- **Database** - H2 in PostgreSQL mode (file-based)

### Key Design Decisions

1. **Database-First with Flyway** - Schema managed via SQL migrations, not JPA
2. **DTO Pattern** - Separate request/response objects for security
3. **Automatic Timestamps** - `@PrePersist` / `@PreUpdate` callbacks
4. **Enum for Status** - Type-safe task statuses
5. **Global Exception Handling** - Consistent error responses
6. **Four-Tier Testing** - Unit, Integration, Functional, Smoke tests

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

### Migrations
Schema is version-controlled with Flyway:
- `V1__create_tasks_table.sql` - Initial schema
- `V2__insert_test_data.sql` - Sample data for development

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
3. **Enable Pagination** - For large task lists
4. **Add Caching** - Redis for frequently accessed tasks
5. **Structured Logging** - JSON format for log aggregation
6. **Monitoring** - Prometheus metrics, distributed tracing
7. **Timezone Handling** - Use `Instant` instead of `LocalDateTime` for multi-timezone support

---

## Built With

Spring Boot best practices including:
- Constructor injection for dependencies
- Database-first schema management
- Comprehensive test coverage
- Global exception handling
- API documentation with Swagger

---

## License

This is a technical test project for HMCTS recruitment.
