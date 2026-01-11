package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.cli.TaskCLI;
import com.ortecfinance.tasklist.repository.InMemoryTaskRepository;
import com.ortecfinance.tasklist.repository.TaskRepository;
import com.ortecfinance.tasklist.service.TaskService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@SpringBootApplication
public class TaskListApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Starting console Application");
            startConsole();
        }
        else {
            SpringApplication.run(TaskListApplication.class, args);
            System.out.println("REST API started. Try: http://localhost:8080/projects");
        }
    }

    private static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out, true);

        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo);

        new TaskCLI(service, in, out).run();
    }

    @Bean
    public TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    public TaskService taskService(TaskRepository taskRepository) {
        return new TaskService(taskRepository);
    }
 }