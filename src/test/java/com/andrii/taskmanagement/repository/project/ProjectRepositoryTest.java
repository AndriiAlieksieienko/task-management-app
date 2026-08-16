package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.specification.ProjectSpecifications;
import org.assertj.core.api.AssertionsForInterfaceTypes;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Save project - valid project - persists and generates id")
    @Sql(
            scripts = "classpath:database/users/add-project-repository-users.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidProject_PersistsProject() {
        Project project = new Project();
        project.setName("New Project");
        project.setDescription("A brand new project");
        project.setStatus(ProjectStatus.INITIATED);
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));

        // ids 1 (admin) and 2 (manager) come from add-project-repository-users.sql
        project.setCreatedBy(userRepository.getReferenceById(1L));
        project.setProjectManager(userRepository.getReferenceById(2L));

        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        Project saved = projectRepository.save(project);

        assertThat(saved.getId()).isNotNull();
        assertThat(projectRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Delete project - existing project - soft deletes and excludes from findById")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void delete_ExistingProject_SoftDeletesProject() {
        Project project = projectRepository.findById(1L).orElseThrow();

        projectRepository.delete(project);

        assertThat(projectRepository.findById(1L)).isEmpty();
        AssertionsForInterfaceTypes.assertThat(projectRepository.findAll())
                .extracting(Project::getId)
                .doesNotContain(1L);
    }

    @Test
    @DisplayName("Find all - name contains - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_NameContains_ReturnsMatchingProjects() {
        Specification<Project> specification = ProjectSpecifications.nameContains("redesign");

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Website Redesign");
    }

    @Test
    @DisplayName("Find all - has status - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasStatus_ReturnsMatchingProjects() {
        Specification<Project> specification = ProjectSpecifications.hasStatus(ProjectStatus.IN_PROGRESS);

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Mobile App Launch");
    }

    @Test
    @DisplayName("Find all - has project manager - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasProjectManager_ReturnsMatchingProjects() {
        // Project 3 is the only one managed by user id 3 (see add-project-repository-projects.sql)
        Specification<Project> specification = ProjectSpecifications.hasProjectManager(3L);

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Data Migration");
    }

    @Test
    @DisplayName("Find all - has member - returns projects containing the member")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql",
                    "classpath:database/project_members/add-project-repository-members.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/project_members/remove-project-repository-members.sql",
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_HasMember_ReturnsProjectsContainingMember() {
        // user id 4 is only a member of project 1 (see add-project-repository-members.sql)
        Specification<Project> specification = ProjectSpecifications.hasMember(4L);

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Website Redesign");
    }

    @Test
    @DisplayName("Find all - start date range - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_StartDateRange_ReturnsMatchingProjects() {
        // Only project 2 starts on/after 2026-02-01 (project 1 starts 2026-01-01,
        // project 3 starts 2025-11-01)
        Specification<Project> specification =
                ProjectSpecifications.startDateFrom(LocalDate.of(2026, 2, 1));

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Mobile App Launch");
    }

    @Test
    @DisplayName("Find all - end date range - returns matching projects")
    @Sql(
            scripts = {
                    "classpath:database/users/add-project-repository-users.sql",
                    "classpath:database/projects/add-project-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/projects/remove-project-repository-projects.sql",
                    "classpath:database/users/remove-project-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAll_EndDateRange_ReturnsMatchingProjects() {
        // Only project 3 ends on/before 2026-03-01 (project 1 ends 2026-06-30,
        // project 2 ends 2026-09-30)
        Specification<Project> specification =
                ProjectSpecifications.endDateTo(LocalDate.of(2026, 3, 1));

        List<Project> actual = projectRepository.findAll(specification);

        AssertionsForInterfaceTypes.assertThat(actual).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(actual)
                .extracting(Project::getName)
                .containsExactly("Data Migration");
    }
}
