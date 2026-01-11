package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.cli.TaskCLI;
import com.ortecfinance.tasklist.service.TaskService;
import org.junit.jupiter.api.*;
import java.io.*;

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.ortecfinance.tasklist.repository.InMemoryTaskRepository;
import com.ortecfinance.tasklist.repository.TaskRepository;

public final class ApplicationTest {
    public static final String PROMPT = "> ";
    private final PipedOutputStream inStream = new PipedOutputStream();
    private final PrintWriter inWriter = new PrintWriter(inStream, true);

    private final PipedInputStream outStream = new PipedInputStream();
    private final BufferedReader outReader = new BufferedReader(new InputStreamReader(outStream));

    private final TaskRepository repository = new InMemoryTaskRepository();

    private Thread applicationThread;

    public ApplicationTest() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new PipedInputStream(inStream)));
        PrintWriter out = new PrintWriter(new PipedOutputStream(outStream), true);

        TaskService service = new TaskService(repository);
        TaskCLI cli = new TaskCLI(service,in, out);
        applicationThread = new Thread(cli);
    }

    @BeforeEach
    public void start_the_application() throws IOException {
        applicationThread.start();
        readLines("Welcome to TaskList! Type 'help' for available commands.");
    }

    @AfterEach
    public void kill_the_application() throws IOException, InterruptedException {
        if (!stillRunning()) {
            return;
        }

        Thread.sleep(1000);
        if (!stillRunning()) {
            return;
        }

        applicationThread.interrupt();
        throw new IllegalStateException("The application is still running.");
    }

    @Test
    void it_works() throws IOException {
        execute("show");

        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("show");
        readLines(
            "secrets",
            "    [ ] 1: Eat more donuts.",
            "    [ ] 2: Destroy all humans.",
            ""
        );

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training SOLID");
        execute("add task training Coupling and Cohesion");
        execute("add task training Primitive Obsession");
        execute("add task training Outside-In TDD");
        execute("add task training Interaction-Driven Design");

        execute("check 1");
        execute("check 3");
        execute("check 5");
        execute("check 6");

        execute("show");
        readLines(
                "secrets",
                "    [x] 1: Eat more donuts.",
                "    [ ] 2: Destroy all humans.",
                "",
                "training",
                "    [x] 3: Four Elements of Simple Design",
                "    [ ] 4: SOLID",
                "    [x] 5: Coupling and Cohesion",
                "    [x] 6: Primitive Obsession",
                "    [ ] 7: Outside-In TDD",
                "    [ ] 8: Interaction-Driven Design",
                ""
        );

        execute("quit");
    }

    @Test
    void it_sets_deadline_for_task() throws IOException {
        execute("add project secrets");
        execute("add task secrets Eat more donuts.");

        execute("deadline 1 31-12-2025");

        execute("show");
        readLines(
            "secrets",
            "    [ ] 1: Eat more donuts. 31-12-2025",
            ""
        );

        execute("quit");
    }

    @Test
    void it_shows_task_due_today_grouped_by_project() throws IOException {
        String todayDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("deadline 1 " + todayDate);

        execute("add project training");
        execute("add task training Learn Java Streams.");
        // no deadline for training task -> should NOT show in today output

        execute("today");
        readLines(
                "secrets",
                "    [ ] 1: Eat more donuts. " + todayDate,
                ""
        );

        execute("quit");
    }

    @Test
    void it_groups_tasks_by_deadline_and_project() throws IOException {
        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training Interaction-Driven Design");

        execute("deadline 1 11-11-2025");
        execute("deadline 3 11-11-2025");
        execute("deadline 4 13-11-2025");

        execute("view-by-deadline");
        readLines(
                "11-11-2025:",
                "    secrets:",
                "        1: Eat more donuts.",
                "    training:",
                "        3: Four Elements of Simple Design",
                "13-11-2025:",
                "    training:",
                "        4: Interaction-Driven Design",
                "No deadline:",
                "    secrets:",
                "        2: Destroy all humans.",
                ""
        );

        execute("quit");
    }


    private void execute(String command) throws IOException {
        read(PROMPT);
        write(command);
    }

    private void read(String expectedOutput) throws IOException {
        int length = expectedOutput.length();
        char[] buffer = new char[length];
        outReader.read(buffer, 0, length);
        assertThat(String.valueOf(buffer), is(expectedOutput));
    }

    private void readLines(String... expectedOutput) throws IOException {
        for (String line : expectedOutput) {
            read(line + lineSeparator());
        }
    }

    private void write(String input) {
        inWriter.println(input);
    }

    private boolean stillRunning() {
        return applicationThread != null && applicationThread.isAlive();
    }
}
