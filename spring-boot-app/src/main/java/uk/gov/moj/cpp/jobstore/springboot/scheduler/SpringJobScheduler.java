package uk.gov.moj.cpp.jobstore.springboot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.jobstore.persistence.Job;
import uk.gov.moj.cpp.jobstore.persistence.Priority;
import uk.gov.moj.cpp.jobstore.service.JobService;
import uk.gov.moj.cpp.jobstore.springboot.config.JobStoreProperties;
import uk.gov.moj.cpp.jobstore.springboot.task.SpringTaskRegistry;
import uk.gov.moj.cpp.task.execution.JobStoreSchedulerPrioritySelector;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Spring Boot scheduler that replaces the EJB @Timeout mechanism.
 * Uses Spring's @Scheduled annotation to periodically check for jobs.
 */
@Component
public class SpringJobScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringJobScheduler.class);
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private SpringTaskRegistry taskRegistry;
    
    @Autowired
    private JobStoreSchedulerPrioritySelector prioritySelector;
    
    @Autowired
    private UtcClock clock;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    @Qualifier("jobExecutorTaskExecutor")
    private TaskExecutor taskExecutor;
    
    @Autowired
    private JobStoreProperties properties;
    
    /**
     * Periodically fetch unassigned jobs from the database and execute them.
     * The interval is configured via jobstore.scheduler.timer-interval property.
     */
    @Scheduled(
            fixedDelayString = "${jobstore.scheduler.timer-interval}",
            initialDelayString = "${jobstore.scheduler.timer-start-wait}"
    )
    public void fetchUnassignedJobs() {
        UUID workerId = UUID.randomUUID();
        List<Priority> orderedPriorities = prioritySelector.selectOrderedPriorities();
        
        if (logger.isDebugEnabled()) {
            logger.debug(format("Fetching new jobs from jobstore in priority order %s", orderedPriorities));
        }
        
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            // Fetch jobs in a transaction
            Stream<Job> unassignedJobs = jobService.getUnassignedJobsFor(workerId, orderedPriorities);
            List<Job> jobList = unassignedJobs.toList();
            
            transactionManager.commit(status);
            
            if (jobList.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No new jobs found in jobstore");
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Found %d %s priority job(s) to run from jobstore", jobList.size(), jobList.get(0).getPriority()));
                }
                execute(jobList);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected exception during job fetch, attempting rollback...", e);
            
            try {
                transactionManager.rollback(status);
                logger.info("Transaction rolled back successfully");
            } catch (Exception e1) {
                logger.error("Unexpected exception during transaction rollback", e1);
            }
        }
    }
    
    private void execute(List<Job> jobsToDo) {
        jobsToDo.forEach(job -> {
            logger.trace("Trigger task execution");
            
            taskExecutor.execute(new SpringJobExecutor(
                    job,
                    taskRegistry,
                    jobService,
                    transactionManager,
                    clock
            ));
            
            logger.trace("Invocation of Task complete");
        });
    }
}

