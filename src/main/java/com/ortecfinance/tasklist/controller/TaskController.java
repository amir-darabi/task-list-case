package com.ortecfinance.tasklist.controller;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // POST /projects
    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody CreateProjectRequest request) {
        String name = request == null ? null : request.name();
        log.info("POST /projects name={}", name);

        if (name == null || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            service.addProject(name);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.info("POST /projects -> 400 ({})", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // GET /projects
    @GetMapping
    public Map<String, List<Task>> getProjects() {
        Map<String, List<Task>> projects = service.getAllProjectsWithTasks();
        log.info("GET /projects -> {} project(s)", projects.size());
        return projects;
    }

    // POST /projects/{projectId}/tasks
    // projectId is currently the project *name* (projects are stored by name in memory).
    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<Void> createTask(
            @PathVariable String projectId,
            @RequestBody CreateTaskRequest request
    ) {
        String description = request == null ? null : request.description();

        if (description == null || description.isBlank()) {
            log.info("POST /projects/{}/tasks -> 400 (missing/blank description)", projectId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        log.info("POST /projects/{}/tasks description={}", projectId, description);

        try {
            service.addTask(projectId, description);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.info("POST /projects/{}/tasks -> 404/400 ({})", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // PUT /projects/{projectId}/tasks/{taskId}?deadline=dd-MM-yyyy
    @PutMapping("/{projectId}/tasks/{taskId}")
    public ResponseEntity<Void> updateTaskDeadline(
            @PathVariable String projectId,
            @PathVariable long taskId,
            @RequestParam("deadline") String deadline
    ) {
        //projectId is currently not used because tasks are looked up by ID
        log.info("PUT /projects/{}/tasks/{} deadline={}", projectId, taskId, deadline);

        LocalDate date = LocalDate.parse(deadline, DEADLINE_FORMATTER);
        service.setTaskDeadline(taskId, date);

        return ResponseEntity.noContent().build();
    }

    // GET /projects/view_by_deadline
    @GetMapping("/view_by_deadline")
    public TaskService.DeadlineView viewByDeadline() {
        TaskService.DeadlineView view = service.getTasksByDeadline();
        log.info("GET /projects/view_by_deadline");
        return view;
    }

    public record CreateProjectRequest(String name) {}
    public record CreateTaskRequest(String description) {}
}