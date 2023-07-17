package com.jblog.hystrix.connector;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskConnector {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "CircuitBreakerService", fallbackMethod = "getTaskDetailsFallback")
    public String getTaskDetails(Integer id) {
        return restTemplate.getForObject("/task/{id}", String.class, id);
    }

    @CircuitBreaker(name = "RetryService", fallbackMethod = "getTaskDetailsFallback")
    public String getTaskDetailsRetry(Integer id) {
        return restTemplate.getForObject("/task/{id}", String.class, id);
    }

    public String getTaskDetailsFallback(Integer id, Throwable error) {
        return "Default fallback with error " + error.getClass().getName();
    }

}
