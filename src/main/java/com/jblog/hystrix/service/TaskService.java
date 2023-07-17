package com.jblog.hystrix.service;

import com.jblog.hystrix.connector.TaskConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskConnector taskConnector;

    public String getTaskDetailsCircuitBreaker(Integer id) {
        log.info("Getting task {} details using Circuit Breaker pattern", id);
        // some other business logic here
        return taskConnector.getTaskDetailsCircuitBreaker(id);
    }

    public String getTaskDetailsRetry(Integer id) {
        log.info("Getting task {} details using Retry pattern", id);
        // some other business logic here
        return taskConnector.getTaskDetailsRetry(id);
    }

    public String getTaskDetailsBulkhead(Integer id) {
        log.info("Getting task {} details using Bulkhead pattern", id);
        // some other business logic here
        return taskConnector.getTaskDetailsBulkhead(id);
    }

    public String getTaskDetailsRatelimiter(Integer id) {
        log.info("Getting task {} details using Ratelimiter pattern", id);
        // some other business logic here
        return taskConnector.getTaskDetailsRatelimiter(id);
    }
}
