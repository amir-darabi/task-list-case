package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;

public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public void addProject(String name) {
        repository.addProject(name);
    }

    public void addTask(String project, String description) {
        if (!repository.hasProject(project)) {
            throw new IllegalStateException(
                    String.format("Could not find a project with the name \"%s\".", project)
            );
        }
        repository.addTask(project, description);
    }

    public void setTaskDone(long id, boolean done) {
        Task task = repository.findTaskById(id)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Could not find a task with an ID of %d.", id)
                ));
        task.setDone(done);
    }

    public void setTaskDeadline(long id, LocalDate date) {
        Task task = repository.findTaskById(id)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Could not find a task with an ID of %d.", id)
                ));
        task.setDeadline(date);
    }

    public Map<String, List<Task>> getAllProjectsWithTasks() {
        return repository.getAllProjectsWithTasks();
    }

    // DTO for transferring deadline viewdata
    public class DeadlineView {
        public final Map<LocalDate, Map<String, List<Task>>> byDeadline;
        public final Map<String, List<Task>> noDeadline;

        public DeadlineView(
                Map<LocalDate, Map<String, List<Task>>> byDeadline,
                Map<String, List<Task>> noDeadline
        ) {
            this.byDeadline = byDeadline;
            this.noDeadline = noDeadline;
        }
    }

    public DeadlineView getTasksByDeadline() {
        Map<LocalDate, Map<String, List<Task>>> byDeadline = new TreeMap<>();
        Map<String, List<Task>> noDeadline = new LinkedHashMap<>();

        for (Map.Entry<String, List<Task>> project : repository.getAllProjectsWithTasks().entrySet()) {
            String projectName = project.getKey();
            for (Task task : project.getValue()) {
                if (task.getDeadline() == null) {
                    noDeadline
                            .computeIfAbsent(projectName, k -> new ArrayList<>())
                            .add(task);
                } else {
                    byDeadline
                            .computeIfAbsent(task.getDeadline(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(projectName, k -> new ArrayList<>())
                            .add(task);
                }
            }
        }
        return new DeadlineView(byDeadline, noDeadline);
    }

    public Map<String, List<Task>> getTodaysTasks() {
        LocalDate todayDate = LocalDate.now();
        Map<String, List<Task>> todayTasks = new LinkedHashMap<>();

        for (Map.Entry<String, List<Task>> project : repository.getAllProjectsWithTasks().entrySet()) {
            List<Task> tasksForToday = project.getValue().stream()
                    .filter(task -> task.getDeadline() != null && task.getDeadline().equals(todayDate))
                    .toList();
            if (!tasksForToday.isEmpty()) {
                todayTasks.put(project.getKey(), tasksForToday);
            }
        }
        return todayTasks;
    }
}
