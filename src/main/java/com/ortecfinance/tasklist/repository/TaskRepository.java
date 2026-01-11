package com.ortecfinance.tasklist.repository;

import com.ortecfinance.tasklist.model.Task;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface TaskRepository {
    void addProject(String name);

    boolean hasProject(String name);

    Task addTask(String projectName, String description);

    Map<String, List<Task>> getAllProjectsWithTasks();

    Optional<Task> findTaskById(long id);
}
