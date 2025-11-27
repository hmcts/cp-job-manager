package uk.gov.moj.cpp.jobstore.springboot.adapters;

import uk.gov.moj.cpp.jobstore.springboot.config.JobStoreProperties;

/**
 * Adapter that implements the CDI-based JobStoreConfiguration interface
 * using Spring Boot properties.
 */
public class SpringJobStoreConfiguration extends uk.gov.moj.cpp.jobstore.persistence.JobStoreConfiguration {
    
    private final JobStoreProperties properties;
    
    public SpringJobStoreConfiguration(JobStoreProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public long getTimerStartWaitMilliseconds() {
        return properties.getScheduler().getTimerStartWait();
    }
    
    @Override
    public long getTimerIntervalMilliseconds() {
        return properties.getScheduler().getTimerInterval();
    }
    
    @Override
    public int getJobPriorityPercentageHigh() {
        return properties.getJob().getPriority().getPercentage().getHigh();
    }
    
    @Override
    public int getJobPriorityPercentageLow() {
        return properties.getJob().getPriority().getPercentage().getLow();
    }
    
    @Override
    public int getWorkerJobCount() {
        return properties.getScheduler().getWorkerJobCount();
    }
    
    @Override
    public String getModuleName() {
        return properties.getModuleName();
    }
}

