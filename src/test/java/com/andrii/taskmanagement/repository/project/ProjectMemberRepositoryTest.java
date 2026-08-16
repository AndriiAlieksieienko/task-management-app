package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import java.util.List;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
        scripts = {
                "classpath:database/users/add-project-member-repository-users.sql",
                "classpath:database/projects/add-project-member-repository-projects.sql",
                "classpath:database/project_members/add-project-member-repository-members.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = {
                "classpath:database/project_members/remove-project-member-repository-members.sql",
                "classpath:database/projects/remove-project-member-repository-projects.sql",
                "classpath:database/users/remove-project-member-repository-users.sql"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class ProjectMemberRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    @DisplayName("Exists by project id and user id - existing membership - returns true")
    void existsByProjectIdAndUserId_ExistingMembership_ReturnsTrue() {
        boolean actual = projectMemberRepository.existsByProjectIdAndUserId(1L, 3L);

        Assertions.assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Exists by project id and user id - nonexistent membership - returns false")
    void existsByProjectIdAndUserId_NonexistentMembership_ReturnsFalse() {
        boolean actual = projectMemberRepository.existsByProjectIdAndUserId(2L, 4L);

        Assertions.assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Find all by project ids with users - multiple project ids - returns members with users fetched")
    void findAllByProjectIdsWithUsers_MultipleProjectIds_ReturnsMembersWithUsers() {
        List<ProjectMember> actual =
                projectMemberRepository.findAllByProjectIdsWithUsers(List.of(1L, 2L));

        Assertions.assertThat(actual).hasSize(3);
        Assertions.assertThat(actual)
                .extracting(pm -> pm.getProject().getId())
                .containsExactlyInAnyOrder(1L, 1L, 2L);
        Assertions.assertThat(actual)
                .extracting(pm -> pm.getUser().getId())
                .containsExactlyInAnyOrder(3L, 4L, 3L);
        Assertions.assertThat(actual)
                .extracting(pm -> pm.getUser().getUsername())
                .containsExactlyInAnyOrder(
                        "pm-repo-member@gmail.com",
                        "pm-repo-member-2@gmail.com",
                        "pm-repo-member@gmail.com"
                );
    }

    @Test
    @DisplayName("Find all by project ids with users - unknown project id - returns empty list")
    void findAllByProjectIdsWithUsers_UnknownProjectId_ReturnsEmptyList() {
        List<ProjectMember> actual =
                projectMemberRepository.findAllByProjectIdsWithUsers(List.of(999L));

        Assertions.assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Delete by project id and user id - existing membership - deletes only that row")
    void deleteByIdProjectIdAndIdUserId_ExistingMembership_DeletesOnlyThatRow() {
        projectMemberRepository.deleteByIdProjectIdAndIdUserId(1L, 3L);

        Assertions.assertThat(projectMemberRepository.existsByProjectIdAndUserId(1L, 3L)).isFalse();
        Assertions.assertThat(projectMemberRepository.existsByProjectIdAndUserId(1L, 4L)).isTrue();
        Assertions.assertThat(projectMemberRepository.existsByProjectIdAndUserId(2L, 3L)).isTrue();
    }
}
