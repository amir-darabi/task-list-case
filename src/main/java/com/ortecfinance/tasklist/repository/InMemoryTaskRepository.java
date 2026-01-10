package com.ortecfinance.tasklist.repository;

import com.ortecfinance.tasklist.model.Task;

import java.util.*;

public final class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, List<Task>> tasksByProject = new LinkedHashMap<>();
    private long lastId = 0;

    @Override
    public void addProject(String name) {
        // avoid overwriting existing project
        tasksByProject.putIfAbsent(name, new ArrayList<>());
    }

    @Override
    public boolean hasProject(String name) {
        return tasksByProject.containsKey(name);
    }

    @Override
    public Task addTask(String projectName, String description) {
        List<Task> projectTasks = tasksByProject.get(projectName);
        if (projectTasks == null) {
            throw new IllegalArgumentException("Project does not exist: " + projectName);
        }

        Task task = new Task(nextId(), description, false);
        projectTasks.add(task);
        return task;
    }

    @Override
    public Map<String, List<Task>> getAllProjectsWithTasks() {
        return tasksByProject;
    }

    @Override
    // Optional for handling absence of task
    public Optional<Task> findTaskById(long id) {
        for (List<Task> projectTasks : tasksByProject.values()) {
            for (Task task : projectTasks) {
                if (task.getId() == id) {
                    return Optional.of(task);
                }
            }
        }
        return Optional.empty();
    }

    private long nextId(){
        return ++lastId;
    }
}
