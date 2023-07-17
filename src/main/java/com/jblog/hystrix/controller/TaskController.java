package com.jblog.hystrix.controller;

import com.jblog.hystrix.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/task/{id}")
    public String getTaskDetails(@PathVariable("id") Integer id) {
        return taskService.getTaskDetails(id);
    }
}
