package uk.gov.moj.cpp.jobstore.springboot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Job;
import uk.gov.moj.cpp.jobstore.service.JobService;
import uk.gov.moj.cpp.jobstore.springboot.task.SpringTaskRegistry;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;

/**
 * Spring Boot implementation of JobExecutor.
 * Executes a single job using Spring's transaction management.
 */
public class SpringJobExecutor implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringJobExecutor.class);
    
    private final Job job;
    private final SpringTaskRegistry taskRegistry;
    private final JobService jobService;
    private final PlatformTransactionManager transactionManager;
    private final UtcClock clock;
    
    public SpringJobExecutor(
            Job job,
            SpringTaskRegistry taskRegistry,
            JobService jobService,
            PlatformTransactionManager transactionManager,
            UtcClock clock) {
        this.job = job;
        this.taskRegistry = taskRegistry;
        this.jobService = jobService;
        this.transactionManager = transactionManager;
        this.clock = clock;
    }
    
    @Override
    public void run() {
        String taskName = job.getNextTask();
        logger.info("Invoking {} task", taskName);
        
        Optional<ExecutableTask> task = taskRegistry.getTask(taskName);
        
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            if (task.isPresent()) {
                ExecutionInfo executionInfo = executionInfo().fromJob(job).build();
                
                if (isStartTimeOfTask(executionInfo)) {
                    executeTask(task.get(), executionInfo);
                }
            } else {
                logger.error("No task registered to process this job {}", job.getJobId());
                jobService.releaseJob(job.getJobId());
            }
            
            transactionManager.commit(status);
            
        } catch (Exception e) {
            logger.error("Unexpected exception during job execution for Job {}, attempting rollback...", job.getJobId(), e);
            
            try {
                transactionManager.rollback(status);
                logger.info("Transaction rolled back successfully");
            } catch (Exception e1) {
                logger.error("Unexpected exception during transaction rollback", e1);
            }
        }
    }
    
    private boolean isStartTimeOfTask(ExecutionInfo executionInfo) {
        ZonedDateTime nextTaskStartTime = executionInfo.getNextTaskStartTime();
        ZonedDateTime now = clock.now();
        return nextTaskStartTime.isBefore(now) || nextTaskStartTime.isEqual(now);
    }
    
    private void executeTask(ExecutableTask task, ExecutionInfo executionInfo) {
        ExecutionInfo executionResponse = task.execute(executionInfo);
        
        if (executionResponse.getExecutionStatus().equals(INPROGRESS)) {
            if (canRetry(task, executionResponse)) {
                performRetry(task);
            } else {
                Integer retryAttemptsRemaining = taskRegistry.findRetryAttemptsRemainingFor(executionResponse.getNextTask());
                jobService.updateJobTaskData(job.getJobId(), executionResponse.getJobData());
                jobService.updateNextTaskDetails(
                        job.getJobId(),
                        executionResponse.getNextTask(),
                        executionResponse.getNextTaskStartTime(),
                        retryAttemptsRemaining
                );
                jobService.releaseJob(job.getJobId());
            }
        } else if (executionResponse.getExecutionStatus().equals(COMPLETED)) {
            jobService.deleteJob(job.getJobId());
        }
    }
    
    private boolean canRetry(ExecutableTask task, ExecutionInfo taskResponse) {
        boolean shouldRetryTask = taskResponse.isShouldRetry();
        Integer retryAttemptsRemaining = job.getRetryAttemptsRemaining();
        boolean taskHasRetryDurationsConfigured = task.getRetryDurationsInSecs().isPresent();
        
        logger.info(
                "Checking if task is retryable, jobID:{}, executionInfo.shouldRetry:{}, retryAttemptsRemaining:{}, has task configured with retryDurationsInSecs:{}",
                job.getJobId(), shouldRetryTask, retryAttemptsRemaining, taskHasRetryDurationsConfigured
        );
        
        return shouldRetryTask && retryAttemptsRemaining > 0 && taskHasRetryDurationsConfigured;
    }
    
    private void performRetry(ExecutableTask currentTask) {
        List<Long> retryDurations = currentTask.getRetryDurationsInSecs().get();
        Integer retryAttemptsRemaining = job.getRetryAttemptsRemaining();
        Long retryDurationInSecs = retryDurations.get(retryDurations.size() - retryAttemptsRemaining);
        ZonedDateTime exhaustTaskStartTime = clock.now().plusSeconds(retryDurationInSecs);
        
        logger.info(
                "Updating task retryDetails to performRetry, jobID: {}, retryAttemptsRemaining: {}, taskToExecuteOnRetriesExhaust: {}, exhaustTaskStartTime: {}",
                job.getJobId(), job.getRetryAttemptsRemaining(), job.getNextTask(), exhaustTaskStartTime
        );
        
        jobService.updateNextTaskRetryDetails(job.getJobId(), exhaustTaskStartTime, retryAttemptsRemaining - 1);
        jobService.releaseJob(job.getJobId());
    }
    
    @Override
    public String toString() {
        return "SpringJobExecutor[job=" + job + "]";
    }
}

