package com.ortecfinance.tasklist;

import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.repository.InMemoryTaskRepository;
import com.ortecfinance.tasklist.repository.TaskRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class TaskList implements Runnable {
    private static final String QUIT = "quit";

    private final TaskRepository repository;
    private final BufferedReader in;
    private final PrintWriter out;

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        TaskRepository repository = new InMemoryTaskRepository();
        new TaskList(repository, in, out).run();
    }

    public TaskList(TaskRepository repository, BufferedReader reader, PrintWriter writer) {
        this.repository = repository;
        this.in = reader;
        this.out = writer;
    }

    public void run() {
        out.println("Welcome to TaskList! Type 'help' for available commands.");
        while (true) {
            out.print("> ");
            out.flush();
            String command;
            try {
                command = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (command.equals(QUIT)) {
                break;
            }
            execute(command);
        }
    }

    private void execute(String commandLine) {
        String[] commandRest = commandLine.split(" ", 2);
        String command = commandRest[0];
        switch (command) {
            case "show":
                show();
                break;
            case "add":
                add(commandRest[1]);
                break;
            case "check":
                check(commandRest[1]);
                break;
            case "uncheck":
                uncheck(commandRest[1]);
                break;
            case "deadline":
                deadline(commandRest[1]);
                break;
            case "today":
                today();
                break;
            case "view-by-deadline":
                viewByDeadline();
                break;
            case "help":
                help();
                break;
            default:
                error(command);
                break;
        }
    }

    private void show() {
        for (Map.Entry<String, List<Task>> project : repository.getAllProjectsWithTasks().entrySet()) {
            out.println(project.getKey());
            for (Task task : project.getValue()) {
                String deadlineStr = task.getDeadline() != null ? " " + task.getDeadline().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
                out.printf("    [%c] %d: %s%s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription(), deadlineStr);
            }
            out.println();
        }
    }

    private void add(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        String subcommand = subcommandRest[0];
        if (subcommand.equals("project")) {
            addProject(subcommandRest[1]);
        } else if (subcommand.equals("task")) {
            String[] projectTask = subcommandRest[1].split(" ", 2);
            addTask(projectTask[0], projectTask[1]);
        }
    }

    private void addProject(String name) {
        repository.addProject(name);
    }

    private void addTask(String project, String description) {
        if (!repository.hasProject(project)) {
            out.printf("Could not find a project with the name \"%s\".", project);
            out.println();
            return;
        }
        repository.addTask(project, description);
    }

    private void check(String idString) {
        setDone(idString, true);
    }

    private void uncheck(String idString) {
        setDone(idString, false);
    }

    private void setDone(String idString, boolean done) {
        long id = Integer.parseInt(idString);
        Optional<Task> task = repository.findTaskById(id);
        if (task.isPresent()) {
            task.get().setDone(done);
        } else {
            out.printf("Could not find a task with an ID of %d.", id);
            out.println();
        }
    }

    private void deadline(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        //ADD LATER: if (parts.length < 2) { ... }

        //ADD LATER: try and catch
        long id = Long.parseLong(parts[0]);
        LocalDate deadlineDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        // Go through each task in each project
        Optional<Task> task = repository.findTaskById(id);
        if (task.isPresent()) {
            task.get().setDeadline(deadlineDate);
        } else {
            out.printf("Could not find a task with an ID of %d.", id);
            out.println();
        }
    }

    private void today() {
        LocalDate todayDate = LocalDate.now();
        for (Map.Entry<String, List<Task>> project : repository.getAllProjectsWithTasks().entrySet()) {
            List<Task> todayTasks = project.getValue().stream()
                    .filter(task -> task.getDeadline() != null && task.getDeadline().equals(todayDate)) //first checking Null to avoid NPE
                    .toList();

            if (!todayTasks.isEmpty()) {
                out.println(project.getKey());
                for (Task task : todayTasks) {
                    String deadlineStr = task.getDeadline() != null ? " " + task.getDeadline().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
                    out.printf("    [%c] %d: %s%s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription(), deadlineStr);
                }
            }
        }
        out.println();
    }


    private void viewByDeadline() {
        //for tasks with deadline: deadline -> (project -> tasks )
        Map<LocalDate, Map<String, List<Task>>> tasksByDeadlineAndProject = new TreeMap<>();
        //for tasks with no deadline: project -> tasks
        Map<String, List<Task>> noDeadlineTasksByProject = new LinkedHashMap<>();

        // Separate tasks by deadline
        for (Map.Entry<String, List<Task>> project : repository.getAllProjectsWithTasks().entrySet() ) {
            String projectName = project.getKey();
            for (Task task : project.getValue()) {
                if (task.getDeadline() == null) {
                    noDeadlineTasksByProject.computeIfAbsent(projectName, k -> new ArrayList<>()).add(task);
                } else {
                    tasksByDeadlineAndProject
                            .computeIfAbsent(task.getDeadline(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(projectName, k -> new ArrayList<>())
                            .add(task);
                }
            }
        }

        // Print tasks grouped by deadline and project
        for (Map.Entry<LocalDate, Map<String, List<Task>>> entry : tasksByDeadlineAndProject.entrySet() ) {
            out.println(entry.getKey().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ":");
            for (Map.Entry<String, List<Task>> projectEntry : entry.getValue().entrySet()) {
                out.println("    " + projectEntry.getKey() + ":");
                for (Task task : projectEntry.getValue()) {
                    out.printf("        %d: %s%n", task.getId(), task.getDescription());
                }
            }
        }

        // Print tasks with no deadlines
        if (!noDeadlineTasksByProject.isEmpty()) {
            out.println("No deadline:");
            for (Map.Entry<String, List<Task>> projectEntry : noDeadlineTasksByProject.entrySet()) {
                out.println("    " + projectEntry.getKey() + ":");
                for (Task task : projectEntry.getValue()) {
                    out.printf("        %d: %s%n", task.getId(), task.getDescription());
                }
            }
        }
        out.println();
    }

    private void help() {
        out.println("Commands:");
        out.println("  show");
        out.println("  add project <project name>");
        out.println("  add task <project name> <task description>");
        out.println("  check <task ID>");
        out.println("  uncheck <task ID>");
        out.println("  deadline <task ID> <dd-MM-yyyy>");
        out.println("  today");
        out.println("  view-by-deadline");
        out.println();
    }

    private void error(String command) {
        out.printf("I don't know what the command \"%s\" is.", command);
        out.println();
    }

}
