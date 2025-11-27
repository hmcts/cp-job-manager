# Architecture Overview

This document explains how the Spring Boot application integrates with the jobstore modules.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│                                                                   │
│  ┌────────────────┐         ┌─────────────────────────────┐    │
│  │  REST API      │         │   Scheduler (every 5s)       │    │
│  │  (JobController)│        │   (SpringJobScheduler)       │    │
│  └────────┬───────┘         └────────┬─────────────────────┘    │
│           │                          │                           │
│           └──────────┬───────────────┘                          │
│                      │                                           │
│           ┌──────────▼───────────┐                              │
│           │  ExecutionService     │                              │
│           │  (Spring Adapter)     │                              │
│           └──────────┬────────────┘                             │
│                      │                                           │
│           ┌──────────▼───────────┐                              │
│           │    JobService         │                              │
│           │  (jobstore-persistence)│                            │
│           └──────────┬────────────┘                             │
│                      │                                           │
│           ┌──────────▼───────────┐                              │
│           │   JobRepository       │                              │
│           │   (JDBC)              │                              │
│           └──────────┬────────────┘                             │
│                      │                                           │
└──────────────────────┼───────────────────────────────────────────┘
                       │
              ┌────────▼────────┐
              │   PostgreSQL    │
              │   (job table)   │
              └─────────────────┘
```

## Components

### 1. Spring Boot Layer

#### JobController
- REST API for submitting jobs
- Converts HTTP requests to ExecutionInfo objects
- Entry point for external systems

#### SpringJobScheduler
- Replaces EJB Timer with `@Scheduled` annotation
- Polls database every N milliseconds (configurable)
- Uses Spring's transaction management
- Executes jobs asynchronously using ThreadPoolTaskExecutor

#### SpringJobExecutor
- Executes a single job (Runnable)
- Manages transaction boundaries
- Handles retry logic
- Updates job status

#### SpringExecutionService
- Spring implementation of ExecutionService interface
- Inserts new jobs into the database
- Integrates with TaskRegistry

#### SpringTaskRegistry
- Discovers all `@Task`-annotated beans
- Maintains a registry of available tasks
- Provides task lookup by name

### 2. Adapter Layer

#### SpringJobStoreConfiguration
- Adapts Spring Boot properties to CDI-style JobStoreConfiguration
- Bridges Spring's `@ConfigurationProperties` with jobstore expectations

#### SpringJobStoreDataSourceProvider
- Provides Spring-managed DataSource to jobstore components
- Replaces JNDI lookup with Spring dependency injection

### 3. JobStore Layer (existing modules)

#### jobstore-api
- Defines ExecutionService interface
- Contains ExecutableTask, ExecutionInfo, ExecutionStatus
- Task annotation for marking executable tasks

#### jobstore-persistence
- JobRepository interface and JDBC implementation
- Job entity
- Database operations (insert, update, delete, lock, release)

#### job-executor
- Contains priority selection logic
- Default implementations of core interfaces

#### jobstore-liquibase
- Database migration scripts
- Creates `job` table with proper schema

## Data Flow

### Job Submission Flow

```
1. HTTP POST → JobController
2. JobController → SpringExecutionService.executeWith()
3. SpringExecutionService → TaskRegistry (get retry config)
4. SpringExecutionService → JobService.insertJob()
5. JobService → JobRepository.insertJob()
6. JobRepository → PostgreSQL INSERT
```

### Job Execution Flow

```
1. @Scheduled timer fires → SpringJobScheduler.fetchUnassignedJobs()
2. SpringJobScheduler → JobService.getUnassignedJobsFor()
   - JobService locks jobs for this worker
   - Returns stream of locked jobs
3. For each job:
   - Submit to ThreadPoolTaskExecutor
   - SpringJobExecutor.run()
4. SpringJobExecutor:
   - Lookup task in TaskRegistry
   - Check if start time reached
   - Execute task.execute()
5. Task returns ExecutionInfo with status:
   - COMPLETED → Delete job from database
   - INPROGRESS + shouldRetry → Update retry details
   - INPROGRESS + nextTask → Update next task details
```

## Key Design Patterns

### 1. Adapter Pattern
- Spring adapters wrap jobstore components
- Provides Spring-style configuration and DI
- Maintains compatibility with existing code

### 2. Strategy Pattern
- ExecutableTask defines strategy interface
- Tasks implement different execution strategies
- Runtime task selection via TaskRegistry

### 3. Template Method Pattern
- SpringJobExecutor provides execution template
- Tasks implement execute() method
- Consistent error handling and transaction management

### 4. Registry Pattern
- TaskRegistry maintains available tasks
- Decouples task discovery from execution
- Supports dynamic task registration

## Transaction Management

### Spring Transaction Boundaries

1. **Job Polling Transaction**
   ```
   @Transaction
   - SELECT jobs WHERE worker_id IS NULL
   - UPDATE jobs SET worker_id = ? WHERE job_id IN (...)
   - COMMIT
   ```

2. **Job Execution Transaction**
   ```
   @Transaction
   - Execute task logic
   - UPDATE or DELETE job based on result
   - COMMIT or ROLLBACK on error
   ```

### Isolation Strategy

- Jobs are locked to prevent concurrent execution
- Each worker has a unique UUID
- Stale locks (>1 hour) are automatically released
- `FOR UPDATE SKIP LOCKED` prevents lock contention

## Configuration

### Spring Boot Properties

```yaml
jobstore:
  scheduler:
    timer-interval: 5000      # How often to poll (ms)
    timer-start-wait: 2000    # Initial delay (ms)
    worker-job-count: 5       # Jobs per poll
  module-name: spring-boot-jobstore
```

### Thread Pool Configuration

```java
ThreadPoolTaskExecutor:
  corePoolSize: 5
  maxPoolSize: 10
  queueCapacity: 100
```

## Retry Mechanism

### Task-Level Retry Configuration

Tasks can specify retry durations:

```java
@Override
public Optional<List<Long>> getRetryDurationsInSecs() {
    return Optional.of(List.of(5L, 10L, 15L));
}
```

### Retry Process

1. Task returns `INPROGRESS` with `shouldRetry=true`
2. SpringJobExecutor checks:
   - retryAttemptsRemaining > 0
   - task has retry durations configured
3. If retryable:
   - Calculate next retry time
   - Update job with new start time
   - Decrement retryAttemptsRemaining
4. Job will be picked up again at the scheduled time

## Priority System

### Priority Levels

- HIGH
- MEDIUM
- LOW

### Priority Selection

DefaultJobStoreSchedulerPrioritySelector uses weighted random selection:

- 70% chance: HIGH → MEDIUM → LOW
- 20% chance: MEDIUM → HIGH → LOW  
- 10% chance: LOW → MEDIUM → HIGH

This ensures high priority jobs are preferred but prevents starvation of lower priority jobs.

## Scalability Considerations

### Current Design

- Single instance polling
- Shared database for coordination
- Pessimistic locking

### Scaling Options

1. **Horizontal Scaling**
   - Run multiple instances
   - Each polls independently
   - Database locking prevents double execution

2. **Vertical Scaling**
   - Increase thread pool size
   - Increase worker-job-count
   - Monitor database connection pool

3. **Partitioning**
   - Partition jobs by priority
   - Dedicated workers per priority
   - Separate job tables per tenant

## Monitoring and Observability

### Key Metrics to Monitor

1. Job throughput (jobs/second)
2. Job latency (submission to completion)
3. Retry rate
4. Failed jobs
5. Thread pool utilization
6. Database connection pool usage
7. Job table size

### Logging

Application provides structured logging at:
- DEBUG: Job polling, task discovery
- INFO: Job execution, task completion
- WARN: Retries, recoverable errors
- ERROR: Failed jobs, system errors

## Extension Points

1. **Custom Tasks**: Implement ExecutableTask
2. **Custom Priority Selector**: Implement JobStoreSchedulerPrioritySelector
3. **Custom Data Source**: Implement JobStoreDataSourceProvider
4. **Custom Configuration**: Extend JobStoreProperties
5. **Monitoring**: Add Spring Boot Actuator endpoints

