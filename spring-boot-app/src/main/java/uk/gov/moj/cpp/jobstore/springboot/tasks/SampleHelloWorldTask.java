package uk.gov.moj.cpp.jobstore.springboot.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;

/**
 * Sample task that demonstrates how to create an ExecutableTask.
 * This task simply logs a message and marks the execution as completed.
 */
@Component
@Task("SAMPLE_HELLO_WORLD_TASK")
public class SampleHelloWorldTask implements ExecutableTask {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleHelloWorldTask.class);
    
    @Override
    public ExecutionInfo execute(ExecutionInfo executionInfo) {
        logger.info("======================================");
        logger.info("Executing SampleHelloWorldTask");
        logger.info("Job Data: {}", executionInfo.getJobData());
        logger.info("Next Task: {}", executionInfo.getNextTask());
        logger.info("======================================");
        
        // Simulate some work
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("SampleHelloWorldTask completed successfully");
        
        // Return completed status - this will cause the job to be removed from the database
        return ExecutionInfo.executionInfo()
                .from(executionInfo)
                .withExecutionStatus(COMPLETED)
                .build();
    }
}

