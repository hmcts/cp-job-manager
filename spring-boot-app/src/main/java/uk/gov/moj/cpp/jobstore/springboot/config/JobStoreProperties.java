package uk.gov.moj.cpp.jobstore.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring Boot configuration properties for JobStore.
 * This replaces the CDI-based JobStoreConfiguration from jobstore-persistence.
 */
@Component
@ConfigurationProperties(prefix = "jobstore")
public class JobStoreProperties {
    
    private Scheduler scheduler = new Scheduler();
    private String moduleName = "spring-boot-jobstore";
    private Job job = new Job();
    
    public static class Scheduler {
        private long timerInterval = 20000;
        private long timerStartWait = 20000;
        private int workerJobCount = 10;
        
        public long getTimerInterval() {
            return timerInterval;
        }
        
        public void setTimerInterval(long timerInterval) {
            this.timerInterval = timerInterval;
        }
        
        public long getTimerStartWait() {
            return timerStartWait;
        }
        
        public void setTimerStartWait(long timerStartWait) {
            this.timerStartWait = timerStartWait;
        }
        
        public int getWorkerJobCount() {
            return workerJobCount;
        }
        
        public void setWorkerJobCount(int workerJobCount) {
            this.workerJobCount = workerJobCount;
        }
    }
    
    public static class Job {
        private Priority priority = new Priority();
        
        public static class Priority {
            private Percentage percentage = new Percentage();
            
            public static class Percentage {
                private int high = 70;
                private int low = 10;
                
                public int getHigh() {
                    return high;
                }
                
                public void setHigh(int high) {
                    this.high = high;
                }
                
                public int getLow() {
                    return low;
                }
                
                public void setLow(int low) {
                    this.low = low;
                }
            }
            
            public Percentage getPercentage() {
                return percentage;
            }
            
            public void setPercentage(Percentage percentage) {
                this.percentage = percentage;
            }
        }
        
        public Priority getPriority() {
            return priority;
        }
        
        public void setPriority(Priority priority) {
            this.priority = priority;
        }
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public String getModuleName() {
        return moduleName;
    }
    
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public Job getJob() {
        return job;
    }
    
    public void setJob(Job job) {
        this.job = job;
    }
}

