# JobStore Spring Boot Application

A Spring Boot application that integrates the JobStore API, Job Executor, and JobStore Persistence modules to create a working job scheduling and execution system.

## Features

- **Job Scheduling**: Periodically polls the database for unassigned jobs and executes them
- **Task Execution**: Executes tasks asynchronously using Spring's ThreadPoolTaskExecutor
- **Retry Mechanism**: Supports automatic retry with configurable retry durations
- **Priority-based Execution**: Jobs can have HIGH, MEDIUM, or LOW priority
- **PostgreSQL Integration**: Uses PostgreSQL for job storage
- **Liquibase Migrations**: Automatically creates database schema on startup
- **REST API**: Provides REST endpoints to submit jobs

## Architecture

This application bridges Spring Boot with the existing JobStore modules:

- **jobstore-api**: Defines the ExecutionService interface and task-related classes
- **jobstore-persistence**: Provides JDBC-based job repository
- **job-executor**: Contains job execution logic (adapted for Spring)
- **jobstore-liquibase**: Database migration scripts

The Spring Boot application provides:

- **SpringExecutionService**: Spring-based implementation of ExecutionService
- **SpringJobScheduler**: Replaces EJB Timer with Spring's @Scheduled
- **SpringTaskRegistry**: Discovers and registers all @Task-annotated beans
- **REST API**: Submit jobs via HTTP endpoints

## Prerequisites

- Java 21
- PostgreSQL 12+
- Gradle 8.x

## Setup Instructions

### 1. Start PostgreSQL

You can use Docker to start PostgreSQL:

```bash
docker run -d \
  --name jobstore-postgres \
  -e POSTGRES_DB=jobstore \
  -e POSTGRES_USER=jobstore \
  -e POSTGRES_PASSWORD=jobstore \
  -p 5432:5432 \
  postgres:16
```

Or use the docker-compose.yml in the root directory:

```bash
cd ..
docker-compose up -d
```

### 2. Build the Application

From the root directory of the project:

```bash
./gradlew :spring-boot-app:build
```

### 3. Run the Application

```bash
./gradlew :spring-boot-app:bootRun
```

Or run the JAR directly:

```bash
java -jar spring-boot-app/build/libs/spring-boot-app-1.0.0-SNAPSHOT.jar
```

The application will:
- Start on port 8080
- Connect to PostgreSQL at localhost:5432
- Run Liquibase migrations to create the `job` table
- Start the job scheduler (polls every 5 seconds by default)

## Configuration

Configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jobstore
    username: jobstore
    password: jobstore

jobstore:
  scheduler:
    timer-interval: 5000          # Poll interval in milliseconds
    timer-start-wait: 2000        # Initial delay before first poll
    worker-job-count: 5           # Number of jobs to fetch per poll
  module-name: spring-boot-jobstore
```

## Usage

### Submit a Job via REST API

Submit a simple hello world task:

```bash
curl -X POST http://localhost:8080/api/jobs/submit \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "SAMPLE_HELLO_WORLD_TASK",
    "priority": "MEDIUM",
    "data": {
      "message": "Hello from REST API"
    }
  }'
```

Submit a retry task (randomly fails to demonstrate retry):

```bash
curl -X POST http://localhost:8080/api/jobs/submit \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "SAMPLE_RETRY_TASK",
    "priority": "HIGH",
    "data": {
      "attempt": 1
    }
  }'
```

### Check Application Health

```bash
curl http://localhost:8080/api/jobs/health
```

### Submit a Job Programmatically

```java
@Autowired
private ExecutionService executionService;

public void submitJob() {
    JsonObject jobData = Json.createObjectBuilder()
        .add("message", "Hello World")
        .build();
    
    ExecutionInfo executionInfo = ExecutionInfo.executionInfo()
        .withJobData(jobData)
        .withNextTask("SAMPLE_HELLO_WORLD_TASK")
        .withNextTaskStartTime(ZonedDateTime.now())
        .withExecutionStatus(STARTED)
        .withPriority(Priority.MEDIUM)
        .build();
    
    executionService.executeWith(executionInfo);
}
```

## Creating Custom Tasks

To create a custom task:

1. Create a class that implements `ExecutableTask`
2. Annotate it with `@Task("YOUR_TASK_NAME")`
3. Mark it as a Spring bean with `@Component`

Example:

```java
@Component
@Task("MY_CUSTOM_TASK")
public class MyCustomTask implements ExecutableTask {
    
    private static final Logger logger = LoggerFactory.getLogger(MyCustomTask.class);
    
    @Override
    public ExecutionInfo execute(ExecutionInfo executionInfo) {
        logger.info("Executing custom task with data: {}", executionInfo.getJobData());
        
        // Do your work here...
        
        // Return COMPLETED to remove job from database
        return ExecutionInfo.executionInfo()
            .from(executionInfo)
            .withExecutionStatus(COMPLETED)
            .build();
    }
}
```

### Task with Retry

```java
@Component
@Task("MY_RETRY_TASK")
public class MyRetryTask implements ExecutableTask {
    
    private static final List<Long> RETRY_DURATIONS = List.of(10L, 30L, 60L);
    
    @Override
    public ExecutionInfo execute(ExecutionInfo executionInfo) {
        try {
            // Try to do work...
            return ExecutionInfo.executionInfo()
                .fromExecutionInfo(executionInfo)
                .withExecutionStatus(COMPLETED)
                .build();
        } catch (Exception e) {
            // Return INPROGRESS with shouldRetry=true
            return ExecutionInfo.executionInfo()
                .from(executionInfo)
                .withExecutionStatus(INPROGRESS)
                .withShouldRetry(true)
                .build();
        }
    }
    
    @Override
    public Optional<List<Long>> getRetryDurationsInSecs() {
        return Optional.of(RETRY_DURATIONS);
    }
}
```

## Database Schema

The application uses the following table structure (created by Liquibase):

```sql
CREATE TABLE job (
    job_id UUID PRIMARY KEY,
    worker_id UUID,
    worker_lock_time TIMESTAMP,
    next_task VARCHAR(255) NOT NULL,
    next_task_start_time TIMESTAMP NOT NULL,
    job_data JSONB NOT NULL,
    retry_attempts_remaining INTEGER,
    priority VARCHAR(10) NOT NULL
);
```

## How It Works

1. **Job Submission**: Jobs are submitted via the REST API or programmatically using ExecutionService
2. **Job Storage**: Jobs are stored in the PostgreSQL database
3. **Job Polling**: The SpringJobScheduler polls the database every 5 seconds (configurable)
4. **Job Locking**: Jobs are locked to prevent concurrent execution
5. **Job Execution**: Tasks are executed asynchronously in a thread pool
6. **Task Processing**: The task's execute() method is called with the job data
7. **Job Completion**: 
   - If status is COMPLETED, the job is deleted
   - If status is INPROGRESS with retry, the job is rescheduled
   - If status is INPROGRESS without retry, the job is updated with next task details

## Monitoring

Application logs show:
- Job submissions
- Task executions
- Retry attempts
- Errors and exceptions

Example log output:

```
2025-11-27 10:00:00.000  INFO --- [  scheduler-1] c.m.c.j.s.s.SpringJobScheduler          : Found 1 MEDIUM priority job(s) to run from jobstore
2025-11-27 10:00:00.100  INFO --- [job-executor-1] c.m.c.j.s.s.SpringJobExecutor           : Invoking SAMPLE_HELLO_WORLD_TASK task
2025-11-27 10:00:00.200  INFO --- [job-executor-1] c.m.c.j.s.t.SampleHelloWorldTask        : ======================================
2025-11-27 10:00:00.201  INFO --- [job-executor-1] c.m.c.j.s.t.SampleHelloWorldTask        : Executing SampleHelloWorldTask
2025-11-27 10:00:01.300  INFO --- [job-executor-1] c.m.c.j.s.t.SampleHelloWorldTask        : SampleHelloWorldTask completed successfully
```

## Troubleshooting

### Connection refused to PostgreSQL

Make sure PostgreSQL is running and accessible at the configured URL:

```bash
docker ps | grep postgres
```

### Liquibase fails to create tables

Check PostgreSQL logs:

```bash
docker logs jobstore-postgres
```

Ensure the database and user exist:

```bash
docker exec -it jobstore-postgres psql -U jobstore -d jobstore -c "\dt"
```

### Jobs are not being picked up

Check the scheduler configuration in application.yml. Increase logging level:

```yaml
logging:
  level:
    uk.gov.moj.cpp: DEBUG
```

## License

See LICENSE file in the root directory.

