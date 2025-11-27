package uk.gov.moj.cpp.jobstore.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application for JobStore task execution system.
 * This application integrates the jobstore-api, jobstore-persistence, and job-executor
 * modules to provide a working job scheduling and execution system.
 */
@SpringBootApplication
@EnableScheduling
public class JobStoreSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobStoreSpringBootApplication.class, args);
    }
}

