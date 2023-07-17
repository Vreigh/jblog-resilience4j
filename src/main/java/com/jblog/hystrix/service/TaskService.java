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

    public String getTaskDetails(Integer id) {
        log.info("Getting task {} details", id);
        // some other business logic here
        return taskConnector.getTaskDetails(id);
    }
}
