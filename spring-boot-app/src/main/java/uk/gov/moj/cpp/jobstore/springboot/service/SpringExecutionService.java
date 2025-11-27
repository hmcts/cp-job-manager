package uk.gov.moj.cpp.jobstore.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Job;
import uk.gov.moj.cpp.jobstore.service.JobService;
import uk.gov.moj.cpp.jobstore.springboot.task.SpringTaskRegistry;

import java.util.UUID;

import static java.util.Optional.empty;

/**
 * Spring Boot implementation of ExecutionService.
 * This service is used to submit new jobs to the jobstore.
 */
@Service
public class SpringExecutionService implements ExecutionService {
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private SpringTaskRegistry taskRegistry;
    
    @Override
    public void executeWith(ExecutionInfo executionInfo) {
        Integer retryAttemptsRemaining = taskRegistry.findRetryAttemptsRemainingFor(executionInfo.getNextTask());
        Job job = new Job(
                UUID.randomUUID(),
                executionInfo.getJobData(),
                executionInfo.getNextTask(),
                executionInfo.getNextTaskStartTime(),
                empty(),
                empty(),
                retryAttemptsRemaining,
                executionInfo.getPriority()
        );
        jobService.insertJob(job);
    }
}

