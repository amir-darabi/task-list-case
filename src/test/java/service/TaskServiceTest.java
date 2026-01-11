package service;

import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.repository.InMemoryTaskRepository;
import com.ortecfinance.tasklist.repository.TaskRepository;
import com.ortecfinance.tasklist.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskServiceTest {

    private TaskRepository repository;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
        service = new TaskService(repository);
    }

    // 1) Adding operations: projects and tasks

    @Test
    void it_creates_project() {
        service.addProject("secrets");

        Map<String, List<Task>> all = service.getAllProjectsWithTasks();
        assertThat(all, hasKey("secrets"));
        assertThat(all.get("secrets"), is(empty()));
    }

    @Test
    void it_adds_task_to_existing_project() {
        service.addProject("secrets");

        service.addTask("secrets", "Eat more donuts.");

        List<Task> tasks = service.getAllProjectsWithTasks().get("secrets");
        assertThat(tasks, hasSize(1));

        Task t = tasks.get(0);
        assertThat(t.getDescription(), is("Eat more donuts."));
        assertThat(t.isDone(), is(false));
        assertThat(t.getDeadline(), is(nullValue()));
    }

    // 2) Validation / error cases

    @Test
    void it_throws_when_adding_task_to_missing_project() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.addTask("missing", "Do something")
        );

        assertThat(ex.getMessage(), is("Could not find a project with the name \"missing\"."));
    }

    @Test
    void it_throws_when_marking_missing_task_done() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.setTaskDone(999, true)
        );

        assertThat(ex.getMessage(), is("Could not find a task with an ID of 999."));
    }

    @Test
    void it_throws_when_setting_deadline_on_missing_task() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.setTaskDeadline(999, LocalDate.of(2025, 12, 31))
        );

        assertThat(ex.getMessage(), is("Could not find a task with an ID of 999."));
    }

    // 3) Update operations

    @Test
    void it_marks_task_as_done() {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        Task t = service.getAllProjectsWithTasks().get("secrets").get(0);

        service.setTaskDone(t.getId(), true);

        assertThat(t.isDone(), is(true));
    }

    @Test
    void it_sets_deadline_on_task() {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        Task t = service.getAllProjectsWithTasks().get("secrets").get(0);
        LocalDate d = LocalDate.of(2025, 12, 31);

        service.setTaskDeadline(t.getId(), d);

        assertThat(t.getDeadline(), is(d));
    }

    // 4) Queries: today and view-by-deadline

    @Test
    void it_returns_only_projects_with_tasks_due_today() {
        LocalDate today = LocalDate.now();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        Task secretsTask = service.getAllProjectsWithTasks().get("secrets").get(0);
        service.setTaskDeadline(secretsTask.getId(), today);

        service.addProject("training");
        service.addTask("training", "Learn Streams.");
        // no deadline => should not appear

        Map<String, List<Task>> result = service.getTodaysTasks();

        assertThat(result.keySet(), contains("secrets"));
        assertThat(result.get("secrets"), hasSize(1));
        assertThat(result.get("secrets").get(0).getDescription(), is("Eat more donuts."));
        assertThat(result.get("secrets").get(0).getDeadline(), is(today));
    }

    @Test
    void it_groups_tasks_by_deadline_then_project_and_separates_no_deadline() {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");     // task 1
        service.addTask("secrets", "Destroy all humans.");  // task 2

        service.addProject("training");
        service.addTask("training", "Four Elements of Simple Design"); // task 3
        service.addTask("training", "Interaction-Driven Design");      // task 4

        Task t1 = service.getAllProjectsWithTasks().get("secrets").get(0);
        Task t2 = service.getAllProjectsWithTasks().get("secrets").get(1);
        Task t3 = service.getAllProjectsWithTasks().get("training").get(0);
        Task t4 = service.getAllProjectsWithTasks().get("training").get(1);

        LocalDate d1 = LocalDate.of(2025, 11, 11);
        LocalDate d2 = LocalDate.of(2025, 11, 13);

        service.setTaskDeadline(t1.getId(), d1);
        service.setTaskDeadline(t3.getId(), d1);
        service.setTaskDeadline(t4.getId(), d2);
        // t2 has no deadline

        TaskService.DeadlineView view = service.getTasksByDeadline();

        // deadlines exist
        assertThat(view.byDeadline, hasKey(d1));
        assertThat(view.byDeadline, hasKey(d2));

        // grouped by project under the deadline
        assertThat(view.byDeadline.get(d1), hasKey("secrets"));
        assertThat(view.byDeadline.get(d1), hasKey("training"));

        // check a couple tasks exist in the right groups
        assertThat(view.byDeadline.get(d1).get("secrets"), hasSize(1));
        assertThat(view.byDeadline.get(d1).get("training"), hasSize(1));
        assertThat(view.byDeadline.get(d2).get("training"), hasSize(1));

        // no deadline group exists
        assertThat(view.noDeadline, hasKey("secrets"));
        assertThat(view.noDeadline.get("secrets"), hasSize(1));
        assertThat(view.noDeadline.get("secrets").get(0).getDescription(), is("Destroy all humans."));
    }
}
