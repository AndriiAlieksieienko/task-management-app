package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.user.UserRepository;
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
class ProjectMemberRepositoryTest extends AbstractRepositoryTest {

    private static final String MEMBER_EMAIL =
            "user-test-member@gmail.com";

    private static final String TARGET_EMAIL =
            "user-test-target@gmail.com";

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Exists by project id and user id - existing membership - returns true")
    void existsByProjectIdAndUserId_ExistingMembership_ReturnsTrue() {

        User member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        boolean actual =
                projectMemberRepository.existsByProjectIdAndUserId(
                        1L,
                        member.getId()
                );

        Assertions.assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Exists by project id and user id - nonexistent membership - returns false")
    void existsByProjectIdAndUserId_NonexistentMembership_ReturnsFalse() {

        User target = userRepository
                .findByEmail(TARGET_EMAIL)
                .orElseThrow();

        boolean actual =
                projectMemberRepository.existsByProjectIdAndUserId(
                        2L,
                        target.getId()
                );

        Assertions.assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Find all by project ids with users - multiple project ids - returns members with users fetched")
    void findAllByProjectIdsWithUsers_MultipleProjectIds_ReturnsMembersWithUsers() {

        User member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        User target = userRepository
                .findByEmail(TARGET_EMAIL)
                .orElseThrow();

        List<ProjectMember> actual =
                projectMemberRepository.findAllByProjectIdsWithUsers(
                        List.of(1L, 2L)
                );

        Assertions.assertThat(actual)
                .hasSize(3);

        Assertions.assertThat(actual)
                .extracting(pm -> pm.getProject().getId())
                .containsExactlyInAnyOrder(
                        1L,
                        1L,
                        2L
                );

        Assertions.assertThat(actual)
                .extracting(pm -> pm.getUser().getId())
                .containsExactlyInAnyOrder(
                        member.getId(),
                        target.getId(),
                        member.getId()
                );

        Assertions.assertThat(actual)
                .extracting(pm -> pm.getUser().getEmail())
                .containsExactlyInAnyOrder(
                        MEMBER_EMAIL,
                        TARGET_EMAIL,
                        MEMBER_EMAIL
                );
    }

    @Test
    @DisplayName("Find all by project ids with users - unknown project id - returns empty list")
    void findAllByProjectIdsWithUsers_UnknownProjectId_ReturnsEmptyList() {

        List<ProjectMember> actual =
                projectMemberRepository.findAllByProjectIdsWithUsers(
                        List.of(999L)
                );

        Assertions.assertThat(actual)
                .isEmpty();
    }

    @Test
    @DisplayName("Delete by project id and user id - existing membership - deletes only that row")
    void deleteByIdProjectIdAndIdUserId_ExistingMembership_DeletesOnlyThatRow() {

        User member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        User target = userRepository
                .findByEmail(TARGET_EMAIL)
                .orElseThrow();

        projectMemberRepository.deleteByIdProjectIdAndIdUserId(
                1L,
                member.getId()
        );

        Assertions.assertThat(
                projectMemberRepository.existsByProjectIdAndUserId(
                        1L,
                        member.getId()
                )
        ).isFalse();

        Assertions.assertThat(
                projectMemberRepository.existsByProjectIdAndUserId(
                        1L,
                        target.getId()
                )
        ).isTrue();

        Assertions.assertThat(
                projectMemberRepository.existsByProjectIdAndUserId(
                        2L,
                        member.getId()
                )
        ).isTrue();
    }
}
