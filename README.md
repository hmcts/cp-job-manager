# cp-job-manager

A stateful multithreaded job and task executor for HMCTS.



A job+task execution engine capable of managing multiple concurrent workloads.
Execution state tracking (enqueue → running → complete/fail)
Thread pool-based parallel processing
Priority awareness

The azure artifacts can be browsed with public access here https://dev.azure.com/hmcts/Artifacts/_artifacts/feed/hmcts-lib Once a repository artifact has been used locally, it will be visible in our local .gradle repository i.e.

cd $HOME/.gradle
find . -name cp-job-manager.* -ls

## Prerequisites

- Java 21 or higher
- Gradle 9.2+ (wrapper included)
- Docker and Docker Compose (for integration tests)

## Project Structure

This is a multi-module Gradle project using Groovy DSL with the following modules:

- **jobstore-liquibase** - Liquibase database migration scripts and configuration
- **jobstore-persistence** - Data persistence layer with JPA/Hibernate
- **jobstore-api** - Public API interfaces and data models
- **job-executor** - Core job execution engine and task management
- **job-manager-it** - Integration tests and end-to-end testing

## Technology Stack

- **Java 21** - Programming language
- **Gradle 9.2** - Build tool with Groovy DSL
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **JaCoCo** - Code coverage analysis
- **Liquibase** - Database migration tool
- **PostgreSQL** - Database (for persistence layer)
- **OpenEJB** - Java EE container for testing

## Building

Build all modules:
```bash
./gradlew build
```

Build a specific module:
```bash
./gradlew :job-executor:build
./gradlew :jobstore-persistence:build
```

Clean all builds:
```bash
./gradlew clean
```

## Database Setup (for Integration Tests)

Integration tests automatically use **Docker Compose** to manage PostgreSQL. The `com.avast.gradle.docker-compose` plugin automatically starts PostgreSQL before tests and stops it after.

### Automatic Database Management

- **Docker Compose** automatically starts PostgreSQL before integration tests
- **PostgreSQL container** runs on port `5432`
- **Database, user, and password** are automatically configured
- **Containers are automatically stopped** after tests complete

### Connection Details

The tests connect to:
- **Host**: `localhost` (or set `INTEGRATION_HOST_KEY` system property to override)
- **Port**: `55432` (mapped from container port 5432, configurable via `POSTGRES_PORT` system property)
- **Database**: `frameworkjobstore`
- **User**: `framework`
- **Password**: `framework`

**Note**: Port 55432 is used to avoid conflicts with system PostgreSQL on port 5432.

### Docker Compose Configuration

The project includes a `docker-compose.yml` file that defines:
- PostgreSQL 15 Alpine image
- Automatic database initialization
- Health checks to ensure database is ready before tests start

**Note**: Make sure Docker is running before executing integration tests. The plugin will handle starting and stopping PostgreSQL automatically.

**Troubleshooting**: If you encounter errors like "container name already in use" or "permission denied", you may need to clean up stuck containers manually:
```bash
docker-compose -f docker-compose.yml -p cp-job-manager down --remove-orphans -v
docker ps -aq --filter "name=cp-job-manager-postgres" | xargs -r docker rm -f
```

## Testing

Run all tests:
```bash
./gradlew test
```

**Note**: Tests are configured to always run (not use build cache) to ensure database connectivity issues are caught immediately. Docker Compose automatically starts PostgreSQL before tests, so no manual database setup is required.

Run tests for a specific module:
```bash
./gradlew :job-executor:test
./gradlew :jobstore-persistence:test
```

View test reports:
```bash
./gradlew test
# Reports available in: <module>/build/reports/tests/test/index.html
```

### Running Tests Without Database

To skip integration tests that require a database:
```bash
./gradlew build -x test
```

## Code Coverage

Generate JaCoCo code coverage reports:
```bash
./gradlew jacocoTestReport
```

View coverage reports:
```bash
# Reports available in: <module>/build/reports/jacoco/test/html/index.html
```

## Other Useful Commands

List all available tasks:
```bash
./gradlew tasks
```

List projects:
```bash
./gradlew projects
```

Build without tests:
```bash
./gradlew build -x test
```

## Dependencies

The project uses explicit dependency versions managed in the root `build.gradle` file. All versions are centralized in the `ext.versions` map for easy maintenance. Key dependencies include:

**Core Dependencies:**
- **SLF4J 1.7.36** - Logging facade
- **Guava 31.1-jre** - Google Core Libraries
- **Jackson 2.14.2** - JSON processing
- **PostgreSQL Driver 42.5.4** - Database connectivity

**Java EE Dependencies:**
- **Java EE API 8.0** - Java Enterprise Edition API
- **CDI API 2.0** - Contexts and Dependency Injection
- **Transaction API 1.3** - Java Transaction API

**Testing Dependencies:**
- **JUnit 5.9.3** - Testing framework
- **Mockito 4.6.1** - Mocking framework
- **Hamcrest 2.2** - Test matchers
- **OpenEJB** - Java EE container for testing
- **Awaitility 4.2.0** - Async testing utilities
- **Liquibase 4.19.1** - Database migration tool

## Development

### Gradle Configuration

The project uses a `gradle.properties` file to configure:
- **Java 21** as the runtime for Gradle daemon
- **Performance optimizations** (parallel execution, configuration cache, build cache)
- **Memory settings** optimized for the project

### IDE Support

The project is configured to work with:
- **IntelliJ IDEA** - Primary IDE (`.idea` directory ignored)
- **VS Code** - Alternative IDE (`.vscode` directory ignored)
- **Eclipse/STS** - Eclipse-based IDEs (`.project`, `.classpath` ignored)

### Code Quality

- **JUnit 5** for unit and integration testing
- **Mockito 4.6.1** for mocking in tests (Java 21 compatible version)
- **Hamcrest** for test assertions

**Note**: The project is configured to use Java 21 for both compilation and testing via `build.gradle`. All tests are enabled and passing.

## Version

Current version: `1.0.0-SNAPSHOT`

## License

This project is licensed under the terms specified in the LICENSE file.
