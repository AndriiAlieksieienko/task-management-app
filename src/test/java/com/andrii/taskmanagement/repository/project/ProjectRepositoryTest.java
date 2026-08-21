package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.specification.ProjectSpecifications;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String ADMIN_EMAIL =
            "user-test-admin@gmail.com";

    private static final String MANAGER_EMAIL =
            "user-test-manager@gmail.com";

    private static final String MEMBER_EMAIL =
            "user-test-member@gmail.com";

    @Test
    @DisplayName("Save project - valid project - persists and generates id")
    @Sql(
            scripts = "classpath:database/users/insert-user-test-users.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidProject_PersistsProject() {

        User admin = userRepository
                .findByEmail(ADMIN_EMAIL)
                .orElseThrow();

        User manager = userRepository
                .findByEmail(MANAGER_EMAIL)
                .orElseThrow();

        Project project = new Project();

        project.setName("New Project");
        project.setDescription("A brand new project");
        project.setStatus(ProjectStatus.INITIATED);
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setCreatedBy(admin);
        project.setProjectManager(manager);

        LocalDateTime now = LocalDateTime.now();

        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        Project saved = projectRepository.save(project);

        assertThat(saved.getId()).isNotNull();

        assertThat(projectRepository.findById(saved.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("Delete project - existing project - soft deletes and excludes from findById")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void delete_ExistingProject_SoftDeletesProject() {

        Project project = projectRepository
                .findById(1L)
                .orElseThrow();

        projectRepository.delete(project);

        assertThat(projectRepository.findById(1L))
                .isEmpty();

        assertThat(projectRepository.findAll())
                .extracting(Project::getId)
                .doesNotContain(1L);
    }

    @Test
    @DisplayName("Find all - name contains - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_NameContains_ReturnsMatchingProjects() {

        Specification<Project> specification =
                ProjectSpecifications.nameContains("redesign");

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Website Redesign");
    }

    @Test
    @DisplayName("Find all - has status - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasStatus_ReturnsMatchingProjects() {

        Specification<Project> specification =
                ProjectSpecifications.hasStatus(
                        ProjectStatus.IN_PROGRESS
                );

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Mobile App Launch");
    }

    @Test
    @DisplayName("Find all - has project manager - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasProjectManager_ReturnsMatchingProjects() {

        User member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        Specification<Project> specification =
                ProjectSpecifications.hasProjectManager(member.getId());

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Data Migration");
    }

    @Test
    @DisplayName("Find all - has member - returns projects containing the member")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql",
                    "classpath:database/project_members/add-repository-test-members.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/project_members/remove-repository-test-members.sql",
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasMember_ReturnsProjectsContainingMember() {

        User member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        Specification<Project> specification =
                ProjectSpecifications.hasMember(member.getId());

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder(
                        "Website Redesign",
                        "Mobile App Launch"
                );
    }

    @Test
    @DisplayName("Find all - start date range - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_StartDateRange_ReturnsMatchingProjects() {

        Specification<Project> specification =
                ProjectSpecifications.startDateFrom(
                        LocalDate.of(2026, 2, 1)
                );

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Mobile App Launch");
    }

    @Test
    @DisplayName("Find all - end date range - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-repository-test-projects.sql",
                    "classpath:database/users/remove-user-test-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_EndDateRange_ReturnsMatchingProjects() {

        Specification<Project> specification =
                ProjectSpecifications.endDateTo(
                        LocalDate.of(2026, 3, 1)
                );

        List<Project> actual =
                projectRepository.findAll(specification);

        assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Data Migration");
    }
}
