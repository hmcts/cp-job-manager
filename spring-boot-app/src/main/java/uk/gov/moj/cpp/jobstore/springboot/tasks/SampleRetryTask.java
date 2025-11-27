package uk.gov.moj.cpp.jobstore.springboot.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;

/**
 * Sample task that demonstrates retry capability.
 * This task randomly fails to demonstrate the retry mechanism.
 */
@Component
@Task("SAMPLE_RETRY_TASK")
public class SampleRetryTask implements ExecutableTask {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleRetryTask.class);
    private final Random random = new Random();
    
    // Retry after 5 seconds, then 10 seconds, then 15 seconds
    private static final List<Long> RETRY_DURATIONS = List.of(5L, 10L, 15L);
    
    @Override
    public ExecutionInfo execute(ExecutionInfo executionInfo) {
        logger.info("======================================");
        logger.info("Executing SampleRetryTask");
        logger.info("Job Data: {}", executionInfo.getJobData());
        logger.info("======================================");
        
        // Simulate random failure (50% chance)
        boolean shouldFail = random.nextBoolean();
        
        if (shouldFail) {
            logger.warn("SampleRetryTask failed - will retry");
            
            // Return INPROGRESS with shouldRetry=true to trigger retry
            return ExecutionInfo.executionInfo()
                    .from(executionInfo)
                    .withExecutionStatus(INPROGRESS)
                    .withShouldRetry(true)
                    .build();
        } else {
            logger.info("SampleRetryTask completed successfully");
            
            // Return COMPLETED to remove job from database
            return ExecutionInfo.executionInfo()
                    .from(executionInfo)
                    .withExecutionStatus(COMPLETED)
                    .build();
        }
    }
    
    @Override
    public Optional<List<Long>> getRetryDurationsInSecs() {
        return Optional.of(RETRY_DURATIONS);
    }
}

