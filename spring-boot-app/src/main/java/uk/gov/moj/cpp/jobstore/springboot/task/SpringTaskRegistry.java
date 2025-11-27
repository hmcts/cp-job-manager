package uk.gov.moj.cpp.jobstore.springboot.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Boot implementation of TaskRegistry.
 * Discovers and registers all ExecutableTask beans annotated with @Task.
 */
@Component
public class SpringTaskRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringTaskRegistry.class);
    
    private final Map<String, ExecutableTask> taskByNameMap = new HashMap<>();
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        // Find all ExecutableTask beans
        Map<String, ExecutableTask> taskBeans = applicationContext.getBeansOfType(ExecutableTask.class);
        
        for (Map.Entry<String, ExecutableTask> entry : taskBeans.entrySet()) {
            ExecutableTask task = entry.getValue();
            Class<?> taskClass = task.getClass();
            
            // Check if the class has @Task annotation
            Task taskAnnotation = taskClass.getAnnotation(Task.class);
            if (taskAnnotation != null) {
                String taskName = taskAnnotation.value();
                taskByNameMap.put(taskName, task);
                logger.info("Registered task: {} -> {}", taskName, taskClass.getName());
            }
        }
        
        logger.info("Task registry initialized with {} task(s)", taskByNameMap.size());
    }
    
    public Optional<ExecutableTask> getTask(String taskName) {
        return Optional.ofNullable(taskByNameMap.get(taskName));
    }
    
    public Integer findRetryAttemptsRemainingFor(String taskName) {
        return getTask(taskName)
                .map(this::findRetryAttemptsRemainingFor)
                .orElse(0);
    }
    
    private Integer findRetryAttemptsRemainingFor(ExecutableTask task) {
        return task.getRetryDurationsInSecs()
                .map(List::size)
                .orElse(0);
    }
}

