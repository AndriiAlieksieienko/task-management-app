package com.andrii.taskmanagement.repository.comment;

import com.andrii.taskmanagement.model.Comment;
import com.andrii.taskmanagement.repository.AbstractRepositoryTest;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
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
class CommentRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Save comment - valid comment - persists and generates id")
    @Sql(
            scripts = {
                    "classpath:database/users/add-comment-repository-users.sql",
                    "classpath:database/projects/add-comment-repository-projects.sql",
                    "classpath:database/tasks/add-comment-repository-tasks.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/comments/remove-comment-repository-comments.sql",
                    "classpath:database/tasks/remove-comment-repository-tasks.sql",
                    "classpath:database/projects/remove-comment-repository-projects.sql",
                    "classpath:database/users/remove-comment-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidComment_PersistsComment() {
        Comment comment = new Comment();

        // task id 1 and user id 3 come from the tasks/users fixtures above
        comment.setTask(taskRepository.getReferenceById(1L));
        comment.setUser(userRepository.getReferenceById(3L));
        comment.setText("A brand new comment");
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(commentRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Find all by task id - existing task - returns only its comments")
    @Sql(
            scripts = {
                    "classpath:database/users/add-comment-repository-users.sql",
                    "classpath:database/projects/add-comment-repository-projects.sql",
                    "classpath:database/tasks/add-comment-repository-tasks.sql",
                    "classpath:database/comments/add-comment-repository-comments.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/comments/remove-comment-repository-comments.sql",
                    "classpath:database/tasks/remove-comment-repository-tasks.sql",
                    "classpath:database/projects/remove-comment-repository-projects.sql",
                    "classpath:database/users/remove-comment-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByTaskId_ExistingTaskId_ReturnsCommentsForThatTask() {
        List<Comment> taskOneComments = commentRepository.findAllByTaskId(1L);

        assertThat(taskOneComments).hasSize(2);
        assertThat(taskOneComments)
                .extracting(Comment::getText)
                .containsExactlyInAnyOrder(
                        "First comment on task one",
                        "Second comment on task one"
                );

        List<Comment> taskTwoComments = commentRepository.findAllByTaskId(2L);

        assertThat(taskTwoComments).hasSize(1);
        assertThat(taskTwoComments)
                .extracting(Comment::getText)
                .containsExactly("First comment on task two");
    }

    @Test
    @DisplayName("Find all by task id - nonexistent task - returns empty list")
    @Sql(
            scripts = {
                    "classpath:database/users/add-comment-repository-users.sql",
                    "classpath:database/projects/add-comment-repository-projects.sql",
                    "classpath:database/tasks/add-comment-repository-tasks.sql",
                    "classpath:database/comments/add-comment-repository-comments.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = {
                    "classpath:database/comments/remove-comment-repository-comments.sql",
                    "classpath:database/tasks/remove-comment-repository-tasks.sql",
                    "classpath:database/projects/remove-comment-repository-projects.sql",
                    "classpath:database/users/remove-comment-repository-users.sql"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByTaskId_NonexistentTaskId_ReturnsEmptyList() {
        List<Comment> actual = commentRepository.findAllByTaskId(999L);

        assertThat(actual).isEmpty();
    }
}
