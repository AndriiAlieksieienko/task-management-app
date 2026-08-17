package com.andrii.taskmanagement.repository.label;

import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LabelRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Save label - valid label - persists and generates id")
    @Sql(
            scripts = {
                    "classpath:database/users/add-label-repository-users.sql",
                    "classpath:database/projects/add-label-repository-projects.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/labels/remove-label-repository-labels.sql",
                    "classpath:database/projects/remove-label-repository-projects.sql",
                    "classpath:database/users/remove-label-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidLabel_PersistsLabel() {
        Label label = new Label();

        // project id 1 comes from the projects fixture above
        label.setProject(projectRepository.getReferenceById(1L));
        label.setName("New Label");
        label.setColor("#123456");
        label.setCreatedAt(LocalDateTime.now());

        Label saved = labelRepository.save(label);

        assertThat(saved.getId()).isNotNull();
        assertThat(labelRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Find all by project id - existing project - returns only its labels")
    @Sql(
            scripts = {
                    "classpath:database/users/add-label-repository-users.sql",
                    "classpath:database/projects/add-label-repository-projects.sql",
                    "classpath:database/labels/add-label-repository-labels.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/labels/remove-label-repository-labels.sql",
                    "classpath:database/projects/remove-label-repository-projects.sql",
                    "classpath:database/users/remove-label-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByProjectId_ExistingProjectId_ReturnsLabelsForThatProject() {
        List<Label> projectALabels = labelRepository.findAllByProjectId(1L);

        assertThat(projectALabels).hasSize(2);
        assertThat(projectALabels)
                .extracting(Label::getName)
                .containsExactlyInAnyOrder("Bug", "Feature");

        List<Label> projectBLabels = labelRepository.findAllByProjectId(2L);

        assertThat(projectBLabels).hasSize(1);
        assertThat(projectBLabels)
                .extracting(Label::getName)
                .containsExactly("Urgent");
    }

    @Test
    @DisplayName("Find all by project id - nonexistent project - returns empty list")
    @Sql(
            scripts = {
                    "classpath:database/users/add-label-repository-users.sql",
                    "classpath:database/projects/add-label-repository-projects.sql",
                    "classpath:database/labels/add-label-repository-labels.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/labels/remove-label-repository-labels.sql",
                    "classpath:database/projects/remove-label-repository-projects.sql",
                    "classpath:database/users/remove-label-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByProjectId_NonexistentProjectId_ReturnsEmptyList() {
        List<Label> actual = labelRepository.findAllByProjectId(999L);

        assertThat(actual).isEmpty();
    }
}
