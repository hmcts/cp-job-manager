# cp-job-manager

A stateful multithreaded job and task executor for HMCTS.



A job+task execution engine capable of managing multiple concurrent workloads.
Execution state tracking (enqueue → running → complete/fail)
Thread pool-based parallel processing
Priority awareness

## Prerequisites

- Java 17 or higher
- Gradle 8.5+ (wrapper included)

## Project Structure

This is a multi-module Gradle project using Groovy DSL with the following modules:

- **jobstore-liquibase** - Liquibase database migration scripts and configuration
- **jobstore-persistence** - Data persistence layer with JPA/Hibernate
- **jobstore-api** - Public API interfaces and data models
- **job-executor** - Core job execution engine and task management
- **job-manager-it** - Integration tests and end-to-end testing

## Technology Stack

- **Java 17** - Programming language
- **Gradle 8.5** - Build tool with Groovy DSL
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

## Testing

Run all tests:
```bash
./gradlew test
```

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

The project uses the Ministry of Justice (MoJ) common BOM for dependency management:

- **uk.gov.justice:maven-common-bom:17.104.0-M1** - Centralized dependency version management

Key dependencies include:
- **SLF4J** - Logging facade
- **Guava** - Google Core Libraries
- **Jackson** - JSON processing
- **PostgreSQL Driver** - Database connectivity
- **OpenEJB** - Java EE container for testing

## Development

### Gradle Configuration

The project uses a `gradle.properties` file to configure:
- **Java 17** as the runtime for Gradle daemon
- **Performance optimizations** (parallel execution, configuration cache, build cache)
- **Memory settings** optimized for the project

### IDE Support

The project is configured to work with:
- **IntelliJ IDEA** - Primary IDE (`.idea` directory ignored)
- **VS Code** - Alternative IDE (`.vscode` directory ignored)
- **Eclipse/STS** - Eclipse-based IDEs (`.project`, `.classpath` ignored)

### Code Quality

- **JUnit 5** for unit and integration testing
- **Mockito 4.6.1** for mocking in tests (Java 17 compatible version)
- **Hamcrest** for test assertions

**Note**: The project is configured to use Java 17 for both compilation and testing via `gradle.properties`. All tests are enabled and passing.

## Version

Current version: `1.0.0-SNAPSHOT`

## License

This project is licensed under the terms specified in the LICENSE file.
