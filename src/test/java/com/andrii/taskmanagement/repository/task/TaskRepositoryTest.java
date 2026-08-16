package com.andrii.taskmanagement.repository.task;

import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.TaskStatus;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Save task - valid task - persists and generates id")
    @Sql(
            scripts = {
                    "classpath:database/users/add-task-repository-users.sql",
                    "classpath:database/projects/add-task-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/tasks/remove-task-repository-tasks.sql",
                    "classpath:database/projects/remove-task-repository-projects.sql",
                    "classpath:database/users/remove-task-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidTask_PersistsTask() {
        Task task = new Task();

        task.setProject(projectRepository.getReferenceById(1L));
        task.setCreatedBy(userRepository.getReferenceById(1L));
        task.setName("New Task");
        task.setDescription("A brand new task");
        task.setPriority(Priority.MEDIUM);
        task.setStatus(TaskStatus.NOT_STARTED);
        task.setDueDate(LocalDate.of(2026, 5, 1));

        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        Task saved = taskRepository.save(task);

        assertThat(saved.getId()).isNotNull();
        assertThat(taskRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Delete task - existing task - soft deletes and excludes from queries")
    @Sql(
            scripts = {
                    "classpath:database/users/add-task-repository-users.sql",
                    "classpath:database/projects/add-task-repository-projects.sql",
                    "classpath:database/tasks/add-task-repository-tasks.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/tasks/remove-task-repository-tasks.sql",
                    "classpath:database/projects/remove-task-repository-projects.sql",
                    "classpath:database/users/remove-task-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void delete_ExistingTask_SoftDeletesTask() {
        Task task = taskRepository.findById(1L).orElseThrow();

        taskRepository.delete(task);

        assertThat(taskRepository.findById(1L)).isEmpty();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> remainingTasksInProjectA = taskRepository.findAllByProjectId(1L, pageable);

        AssertionsForInterfaceTypes.assertThat(remainingTasksInProjectA.getContent())
                .extracting(Task::getId)
                .doesNotContain(1L);
    }

    @Test
    @DisplayName("Find all by project id - existing project - returns only its tasks")
    @Sql(
            scripts = {
                    "classpath:database/users/add-task-repository-users.sql",
                    "classpath:database/projects/add-task-repository-projects.sql",
                    "classpath:database/tasks/add-task-repository-tasks.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/tasks/remove-task-repository-tasks.sql",
                    "classpath:database/projects/remove-task-repository-projects.sql",
                    "classpath:database/users/remove-task-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByProjectId_ExistingProjectId_ReturnsTasksForThatProject() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> projectATasks = taskRepository.findAllByProjectId(1L, pageable);

        assertThat(projectATasks.getTotalElements()).isEqualTo(2);
        AssertionsForInterfaceTypes.assertThat(projectATasks.getContent())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("Task A1", "Task A2");

        Page<Task> projectBTasks = taskRepository.findAllByProjectId(2L, pageable);

        assertThat(projectBTasks.getTotalElements()).isEqualTo(1);
        AssertionsForInterfaceTypes.assertThat(projectBTasks.getContent())
                .extracting(Task::getName)
                .containsExactly("Task B1");
    }

    @Test
    @DisplayName("Find all by project id - nonexistent project - returns empty page")
    @Sql(
            scripts = {
                    "classpath:database/users/add-task-repository-users.sql",
                    "classpath:database/projects/add-task-repository-projects.sql",
                    "classpath:database/tasks/add-task-repository-tasks.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/tasks/remove-task-repository-tasks.sql",
                    "classpath:database/projects/remove-task-repository-projects.sql",
                    "classpath:database/users/remove-task-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByProjectId_NonexistentProjectId_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> actual = taskRepository.findAllByProjectId(999L, pageable);

        AssertionsForInterfaceTypes.assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
    }
}
