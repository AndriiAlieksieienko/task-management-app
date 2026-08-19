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
        scripts = "classpath:database/users/add-user-repository-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/users/remove-user-repository-users.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Exists by email - existing email - returns true")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        boolean actual = userRepository.existsByEmail("user-repo-admin@gmail.com");

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
        boolean actual = userRepository.existsByUsername("userRepoManager");

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
        Optional<User> actual = userRepository.findByEmail("user-repo-member@gmail.com");

        assertThat(actual).isPresent();
        assertThat(actual.get().getUsername()).isEqualTo("user-repo-member@gmail.com");
        assertThat(actual.get().getRole()).isNotNull();
        assertThat(actual.get().getRole().getName()).isEqualTo(RoleName.ROLE_TEAM_MEMBER);
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
        User user = userRepository.findByEmail("user-repo-member@gmail.com").orElseThrow();

        userRepository.delete(user);

        Optional<User> actual = userRepository.findByEmail("user-repo-member@gmail.com");

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Find by id and role name - matching id and role - returns user")
    void findByIdAndRoleName_MatchingIdAndRole_ReturnsUser() {
        Optional<User> actual = userRepository.findByIdAndRoleName(2L, RoleName.ROLE_PROJECT_MANAGER);

        assertThat(actual).isPresent();
        assertThat(actual.get().getUsername()).isEqualTo("user-repo-manager@gmail.com");
    }

    @Test
    @DisplayName("Find by id and role name - id exists but role does not match - returns empty")
    void findByIdAndRoleName_MismatchedRole_ReturnsEmpty() {
        Optional<User> actual = userRepository.findByIdAndRoleName(2L, RoleName.ROLE_TEAM_MEMBER);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Find by id and role name - nonexistent id - returns empty")
    void findByIdAndRoleName_NonexistentId_ReturnsEmpty() {
        Optional<User> actual = userRepository.findByIdAndRoleName(999L, RoleName.ROLE_ADMIN);

        assertThat(actual).isEmpty();
    }
}
