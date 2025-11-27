package uk.gov.moj.cpp.jobstore.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Priority;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.Map;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;

/**
 * REST controller for submitting jobs to the jobstore.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    
    @Autowired
    private ExecutionService executionService;
    
    /**
     * Submit a new job to be executed.
     * 
     * Example request body:
     * {
     *   "taskName": "SAMPLE_HELLO_WORLD_TASK",
     *   "priority": "MEDIUM",
     *   "data": {
     *     "message": "Hello from REST API"
     *   }
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitJob(@RequestBody JobSubmitRequest request) {
        // Create job data as JsonObject
        JsonObject jobData = Json.createObjectBuilder()
                .add("taskName", request.getTaskName())
                .add("data", Json.createObjectBuilder(request.getData()))
                .build();
        
        // Create execution info
        ExecutionInfo executionInfo = ExecutionInfo.executionInfo()
                .withJobData(jobData)
                .withNextTask(request.getTaskName())
                .withNextTaskStartTime(ZonedDateTime.now())
                .withExecutionStatus(STARTED)
                .withPriority(Priority.valueOf(request.getPriority().toUpperCase()))
                .build();
        
        // Submit to execution service
        executionService.executeWith(executionInfo);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Job submitted successfully",
                "taskName", request.getTaskName()
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
    
    public static class JobSubmitRequest {
        private String taskName;
        private String priority = "MEDIUM";
        private Map<String, Object> data;
        
        public String getTaskName() {
            return taskName;
        }
        
        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}

