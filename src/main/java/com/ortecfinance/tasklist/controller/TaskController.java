package com.ortecfinance.tasklist.controller;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // POST /projects
    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody CreateProjectRequest request) {
        service.addProject(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET /projects
    @GetMapping
    public Map<String, List<Task>> getProjects() {
        return service.getAllProjectsWithTasks();
    }

    public record CreateProjectRequest(String name) {}
}