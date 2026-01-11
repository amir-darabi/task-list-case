package com.ortecfinance.tasklist.controller;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/projects")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // POST /projects
    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody CreateProjectRequest request) {
        log.info("POST /projects name={}", request == null ? null : request.name());
        service.addProject(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET /projects
    @GetMapping
    public Map<String, List<Task>> getProjects() {
        var projects = service.getAllProjectsWithTasks();
        log.info("GET /projects -> {} project(s)", projects.size());
        return projects;
    }

    public record CreateProjectRequest(String name) {}
}