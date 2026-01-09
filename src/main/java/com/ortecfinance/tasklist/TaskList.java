package com.ortecfinance.tasklist;

import org.springframework.cglib.core.Local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class TaskList implements Runnable {
    private static final String QUIT = "quit";

    private final Map<String, List<Task>> tasks = new LinkedHashMap<>();
    private final BufferedReader in;
    private final PrintWriter out;

    private long lastId = 0;

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new TaskList(in, out).run();
    }

    public TaskList(BufferedReader reader, PrintWriter writer) {
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
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
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
        tasks.put(name, new ArrayList<Task>());
    }

    private void addTask(String project, String description) {
        List<Task> projectTasks = tasks.get(project);
        if (projectTasks == null) {
            out.printf("Could not find a project with the name \"%s\".", project);
            out.println();
            return;
        }
        projectTasks.add(new Task(nextId(), description, false));
    }

    private void check(String idString) {
        setDone(idString, true);
    }

    private void uncheck(String idString) {
        setDone(idString, false);
    }

    private void setDone(String idString, boolean done) {
        // Change Later? int -> long
        int id = Integer.parseInt(idString);
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDone(done);
                    return;
                }
            }
        }
        out.printf("Could not find a task with an ID of %d.", id);
        out.println();
    }

    private void deadline(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        //ADD LATER: if (parts.length < 2) { ... }

        //ADD LATER: try and catch
        long id = Long.parseLong(parts[0]);
        LocalDate deadlineDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        // Go through each task in each project
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDeadline(deadlineDate);
                    return;
                }
            }
        }
        out.printf("Could not find a task with an ID of %d.", id);
        out.println();
    }

    private void today() {
        LocalDate todayDate = LocalDate.now();
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
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
        // Map: deadline -> list  of tasks    // TreeMap keep it sorted
        Map<LocalDate , List<Task>> tasksByDeadline = new TreeMap<>();
        List<Task> noDeadlineTasks = new ArrayList<>();

        // Separate tasks by deadline
        for (Map.Entry<String, List<Task>> project : tasks.entrySet() ) {
            for (Task task : project.getValue()) {
                if (task.getDeadline() == null) {
                    noDeadlineTasks.add(task);
                } else {
                    // add task to takesByDeadline, if the key(deadline) does not exist, create new list for that key
                    tasksByDeadline.computeIfAbsent(task.getDeadline(), k -> new ArrayList<>()).add(task);
                }
            }
        }

        // Print tasks grouped by deadline
        for (Map.Entry<LocalDate, List<Task>> entry : tasksByDeadline.entrySet()) {
            out.println(entry.getKey().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ":");
            for (Task task : entry.getValue()) {
                out.printf("    %d: %s%n", task.getId(), task.getDescription());
            }
        }

        // Print tasks with no deadlines
        if (!noDeadlineTasks.isEmpty()) {
            out.println("No deadline:");
            for (Task task : noDeadlineTasks) {
                out.printf("    %d: %s%n", task.getId(), task.getDescription());
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

    private long nextId() {
        return ++lastId;
    }
}
