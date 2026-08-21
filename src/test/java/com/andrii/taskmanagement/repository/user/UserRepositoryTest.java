package com.andrii.taskmanagement.repository.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import java.util.Optional;

import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
        scripts = "classpath:database/users/insert-user-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/users/remove-user-test-users.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserRepositoryTest extends AbstractRepositoryTest {

    private static final String ADMIN_EMAIL = "user-test-admin@gmail.com";
    private static final String MANAGER_EMAIL = "user-test-manager@gmail.com";
    private static final String MEMBER_EMAIL = "user-test-member@gmail.com";

    private static final String ADMIN_USERNAME = "userTestAdmin";
    private static final String MANAGER_USERNAME = "userTestManager";
    private static final String MEMBER_USERNAME = "userTestMember";

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Exists by email - existing email - returns true")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        boolean actual = userRepository.existsByEmail(ADMIN_EMAIL);

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Exists by email - nonexistent email - returns false")
    void existsByEmail_NonexistentEmail_ReturnsFalse() {
        boolean actual = userRepository.existsByEmail("nobody@example.com");

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Exists by username - existing username - returns true")
    void existsByUsername_ExistingUsername_ReturnsTrue() {
        boolean actual = userRepository.existsByUsername(MANAGER_USERNAME);

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Exists by username - nonexistent username - returns false")
    void existsByUsername_NonexistentUsername_ReturnsFalse() {
        boolean actual = userRepository.existsByUsername("nobody");

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("Find by email - existing email - returns user with role fetched")
    void findByEmail_ExistingEmail_ReturnsUserWithRole() {
        Optional<User> actual = userRepository.findByEmail(MEMBER_EMAIL);

        assertThat(actual).isPresent();
        assertThat(actual.get().getUsername()).isEqualTo(MEMBER_EMAIL);
        assertThat(actual.get().getEmail()).isEqualTo(MEMBER_EMAIL);
        assertThat(actual.get().getRole()).isNotNull();
        assertThat(actual.get().getRole().getName())
                .isEqualTo(RoleName.ROLE_TEAM_MEMBER);
    }

    @Test
    @DisplayName("Find by email - nonexistent email - returns empty")
    void findByEmail_NonexistentEmail_ReturnsEmpty() {
        Optional<User> actual = userRepository.findByEmail("nobody@example.com");

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Find by email - soft-deleted user - returns empty")
    void findByEmail_SoftDeletedUser_ReturnsEmpty() {
        User user = userRepository.findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        userRepository.delete(user);

        Optional<User> actual = userRepository.findByEmail(MEMBER_EMAIL);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Find by id and role name - matching id and role - returns user")
    void findByIdAndRoleName_MatchingIdAndRole_ReturnsUser() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL)
                .orElseThrow();

        Optional<User> actual = userRepository.findByIdAndRoleName(
                manager.getId(),
                RoleName.ROLE_PROJECT_MANAGER
        );

        assertThat(actual).isPresent();
        assertThat(actual.get().getUsername()).isEqualTo(MANAGER_EMAIL);
        assertThat(actual.get().getEmail()).isEqualTo(MANAGER_EMAIL);
    }

    @Test
    @DisplayName("Find by id and role name - id exists but role does not match - returns empty")
    void findByIdAndRoleName_MismatchedRole_ReturnsEmpty() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL)
                .orElseThrow();

        Optional<User> actual = userRepository.findByIdAndRoleName(
                manager.getId(),
                RoleName.ROLE_TEAM_MEMBER
        );

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Find by id and role name - nonexistent id - returns empty")
    void findByIdAndRoleName_NonexistentId_ReturnsEmpty() {
        Optional<User> actual = userRepository.findByIdAndRoleName(
                999999L,
                RoleName.ROLE_ADMIN
        );

        assertThat(actual).isEmpty();
    }
}
