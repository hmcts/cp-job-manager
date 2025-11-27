# Quick Start Guide

This guide will help you get the JobStore Spring Boot application up and running quickly.

## Prerequisites

- Java 21
- Docker (for PostgreSQL)
- Gradle 8.x (or use the Gradle wrapper)

## Step 1: Start PostgreSQL

The easiest way is to use Docker:

```bash
cd spring-boot-app
docker-compose up -d
```

This will start PostgreSQL on port 5432 with:
- Database: `jobstore`
- Username: `jobstore`
- Password: `jobstore`

## Step 2: Build the Application

From the project root directory:

```bash
gradle :spring-boot-app:build -x test --no-configuration-cache
```

Or if you have gradle wrapper:

```bash
./gradlew :spring-boot-app:build -x test
```

## Step 3: Run the Application

```bash
gradle :spring-boot-app:bootRun --no-configuration-cache
```

Or run the JAR directly:

```bash
java -jar spring-boot-app/build/libs/spring-boot-app-1.0.0-SNAPSHOT.jar
```

The application will:
1. Start on port 8080
2. Connect to PostgreSQL
3. Run Liquibase migrations to create the `job` table
4. Start the job scheduler (polls every 5 seconds)

## Step 4: Submit a Test Job

Once the application is running, submit a job via REST API:

```bash
curl -X POST http://localhost:8080/api/jobs/submit \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "SAMPLE_HELLO_WORLD_TASK",
    "priority": "MEDIUM",
    "data": {
      "message": "Hello from Quick Start!"
    }
  }'
```

You should see output like:

```json
{
  "status": "success",
  "message": "Job submitted successfully",
  "taskName": "SAMPLE_HELLO_WORLD_TASK"
}
```

## Step 5: Watch the Logs

In the application logs, you'll see the job being picked up and executed:

```
2025-11-27 10:00:05.000  INFO --- [  scheduler-1] ...SpringJobScheduler : Found 1 MEDIUM priority job(s) to run
2025-11-27 10:00:05.100  INFO --- [job-executor-1] ...SpringJobExecutor  : Invoking SAMPLE_HELLO_WORLD_TASK task
2025-11-27 10:00:05.200  INFO --- [job-executor-1] ...SampleHelloWorldTask : ======================================
2025-11-27 10:00:05.201  INFO --- [job-executor-1] ...SampleHelloWorldTask : Executing SampleHelloWorldTask
2025-11-27 10:00:05.202  INFO --- [job-executor-1] ...SampleHelloWorldTask : Job Data: {"taskName":"SAMPLE_HELLO_WORLD_TASK",...}
2025-11-27 10:00:06.300  INFO --- [job-executor-1] ...SampleHelloWorldTask : SampleHelloWorldTask completed successfully
```

## Try the Retry Task

Submit a task that demonstrates retry capability:

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

This task randomly fails 50% of the time and will retry with the configured retry durations (5s, 10s, 15s).

## Verify Database

Connect to PostgreSQL to see the jobs:

```bash
docker exec -it jobstore-postgres psql -U jobstore -d jobstore
```

Then query the job table:

```sql
SELECT job_id, next_task, priority, next_task_start_time FROM job;
```

## Health Check

```bash
curl http://localhost:8080/api/jobs/health
```

## Troubleshooting

### Port 5432 already in use

If PostgreSQL is already running on your machine:

1. Stop the existing PostgreSQL service, or
2. Update `spring-boot-app/src/main/resources/application.yml` to use a different port

### Application won't start

Check the logs for errors:

```bash
gradle :spring-boot-app:bootRun --no-configuration-cache --info
```

### Jobs are not being picked up

1. Check PostgreSQL is running: `docker ps | grep postgres`
2. Check the scheduler configuration in `application.yml`
3. Increase logging: Set `uk.gov.moj.cpp: DEBUG` in application.yml

## Stop the Application

Press `Ctrl+C` to stop the application.

To stop PostgreSQL:

```bash
cd spring-boot-app
docker-compose down
```

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Create your own custom tasks by implementing `ExecutableTask`
- Customize the configuration in `application.yml`
- Explore the REST API endpoints in `JobController.java`

