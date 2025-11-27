package uk.gov.moj.cpp.jobstore.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.justice.datasource.jobstore.JobStoreDataSourceProvider;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.jobstore.persistence.JdbcResultSetStreamer;
import uk.gov.moj.cpp.jobstore.persistence.JobJdbcRepository;
import uk.gov.moj.cpp.jobstore.persistence.JobRepository;
import uk.gov.moj.cpp.jobstore.persistence.PreparedStatementWrapperFactory;
import uk.gov.moj.cpp.jobstore.service.JobService;
import uk.gov.moj.cpp.jobstore.springboot.adapters.SpringJobStoreConfiguration;
import uk.gov.moj.cpp.jobstore.springboot.adapters.SpringJobStoreDataSourceProvider;
import uk.gov.moj.cpp.task.execution.DefaultJobStoreSchedulerPrioritySelector;
import uk.gov.moj.cpp.task.execution.JobStoreSchedulerPrioritySelector;
import uk.gov.moj.cpp.task.execution.RandomPercentageProvider;

import javax.sql.DataSource;

/**
 * Spring Boot configuration for JobStore components.
 * This configuration creates Spring beans for all the CDI components.
 */
@Configuration
public class JobStoreConfiguration {
    
    @Bean
    @Scope("prototype")
    public Logger logger(InjectionPoint injectionPoint) {
        return LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());
    }
    
    @Bean
    public UtcClock utcClock() {
        return new UtcClock();
    }
    
    @Bean
    public JobStoreDataSourceProvider jobStoreDataSourceProvider(DataSource dataSource) {
        return new SpringJobStoreDataSourceProvider(dataSource);
    }
    
    @Bean
    public PreparedStatementWrapperFactory preparedStatementWrapperFactory() {
        return new PreparedStatementWrapperFactory();
    }
    
    @Bean
    public JdbcResultSetStreamer jdbcResultSetStreamer() {
        return new JdbcResultSetStreamer();
    }
    
    @Bean
    public JobRepository jobRepository(
            PreparedStatementWrapperFactory preparedStatementWrapperFactory,
            JdbcResultSetStreamer jdbcResultSetStreamer,
            JobStoreDataSourceProvider jobStoreDataSourceProvider) {
        
        JobJdbcRepository repository = new JobJdbcRepository();
        // Use reflection to set protected/private fields
        setField(repository, "preparedStatementWrapperFactory", preparedStatementWrapperFactory);
        setField(repository, "jdbcResultSetStreamer", jdbcResultSetStreamer);
        setField(repository, "jobStoreDataSourceProvider", jobStoreDataSourceProvider);
        setField(repository, "logger", LoggerFactory.getLogger(JobJdbcRepository.class));
        return repository;
    }
    
    @Bean
    public uk.gov.moj.cpp.jobstore.persistence.JobStoreConfiguration jobStoreConfigurationAdapter(
            JobStoreProperties properties) {
        return new SpringJobStoreConfiguration(properties);
    }
    
    @Bean
    public JobService jobService(
            JobRepository jobRepository,
            uk.gov.moj.cpp.jobstore.persistence.JobStoreConfiguration jobStoreConfiguration) {
        
        JobService service = new JobService();
        setField(service, "jobRepository", jobRepository);
        setField(service, "jobStoreConfiguration", jobStoreConfiguration);
        return service;
    }
    
    @Bean
    public RandomPercentageProvider randomPercentageProvider() {
        return new RandomPercentageProvider();
    }
    
    @Bean
    public JobStoreSchedulerPrioritySelector jobStoreSchedulerPrioritySelector(
            uk.gov.moj.cpp.jobstore.persistence.JobStoreConfiguration jobStoreConfiguration,
            RandomPercentageProvider randomPercentageProvider) {
        
        DefaultJobStoreSchedulerPrioritySelector selector = new DefaultJobStoreSchedulerPrioritySelector();
        setField(selector, "jobStoreConfiguration", jobStoreConfiguration);
        setField(selector, "randomPercentageProvider", randomPercentageProvider);
        return selector;
    }
    
    /**
     * Helper method to set private/protected fields using reflection.
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            } else {
                throw new IllegalArgumentException("Field " + fieldName + " not found in class " + target.getClass());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
    
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }
    
    @Bean(name = "jobExecutorTaskExecutor")
    public TaskExecutor jobExecutorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-executor-");
        executor.initialize();
        return executor;
    }
}

