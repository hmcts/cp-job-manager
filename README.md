# cp-job-manager

A stateful multithreaded job and task executor.


## Prerequisites

- Java 17 or higher
- Gradle 8.5+ (wrapper included)

## Project Structure

This is a multi-module Gradle project with the following modules:

- **jobstore-liquibase** - Liquibase database migration scripts
- **jobstore-persistence** - Data persistence layer
- **jobstore-api** - Public API interfaces
- **job-executor** - Job execution engine
- **job-manager-it** - Integration tests

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


## Version

Current version: `1.0.0-SNAPSHOT`
