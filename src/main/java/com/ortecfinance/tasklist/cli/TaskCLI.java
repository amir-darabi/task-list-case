package com.ortecfinance.tasklist.cli;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.service.TaskService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;




public final class TaskCLI implements Runnable {
    private static final String QUIT = "quit";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final TaskService service;
    private final BufferedReader in;
    private final PrintWriter out;

    public TaskCLI(TaskService service, BufferedReader reader, PrintWriter writer) {
        this.service = service;
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

        // ADD Later: try-catch block to handle exceptions from service layer
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
                setDeadline(commandRest[1]);
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
        for (Map.Entry<String, List<Task>> project : service.getAllProjectsWithTasks().entrySet()) {
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
            service.addProject(subcommandRest[1]);
        } else if (subcommand.equals("task")) {
            String[] projectTask = subcommandRest[1].split(" ", 2);
            service.addTask(projectTask[0], projectTask[1]);
        }
    }

    private void check(String idString) {
        long id = Long.parseLong(idString);
        service.setTaskDone(id, true);    }

    private void uncheck(String idString) {
        long id = Long.parseLong(idString);
        service.setTaskDone(id, false);
    }


    private void setDeadline(String args) {
        String[] parts = args.split(" ", 2);
        long id = Long.parseLong(parts[0]);
        LocalDate date = LocalDate.parse(parts[1], DATE_FORMAT);

        service.setTaskDeadline(id, date);
    }

    private void today() {
        Map<String, List<Task>> todayTasks = service.getTodaysTasks();

        for (Map.Entry<String, List<Task>> project : todayTasks.entrySet()) {
            out.println(project.getKey());
            for (Task task : project.getValue()) {
                String deadlineStr = " " + task.getDeadline().format(DATE_FORMAT);
                out.printf("    [%c] %d: %s%s%n",
                        (task.isDone() ? 'x' : ' '),
                        task.getId(),
                        task.getDescription(),
                        deadlineStr
                );
            }
            out.println();
        }
    }


    private void viewByDeadline() {
        Map<LocalDate, Map<String, List<Task>>> grouped = service.getTasksByDeadline();

        for (Map.Entry<LocalDate, Map<String, List<Task>>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();

            if (date == null) {
                out.println("No deadline:");
            } else {
                out.println(date.format(DATE_FORMAT) + ":");
            }

            Map<String, List<Task>> projects = entry.getValue();
            for (Map.Entry<String, List<Task>> projectEntry : projects.entrySet()) {
                out.println("    " + projectEntry.getKey() + ":");

                for (Task task : projectEntry.getValue()) {
                    out.printf("        %d: %s%n", task.getId(), task.getDescription());
                }
            }

            out.println();
        }
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
