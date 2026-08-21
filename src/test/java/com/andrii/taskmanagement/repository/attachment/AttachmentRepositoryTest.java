package com.andrii.taskmanagement.repository.attachment;

import com.andrii.taskmanagement.model.Attachment;
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
class AttachmentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Save attachment - valid attachment - persists and generates id")
    @Sql(
            scripts = {
                    "classpath:database/clean-data.sql",
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql",
                    "classpath:database/tasks/add-repository-tasks.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:database/clean-data.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void save_ValidAttachment_PersistsAttachment() {

        Attachment attachment = new Attachment();

        attachment.setTask(
                taskRepository.getReferenceById(1L)
        );

        attachment.setUploadedBy(
                userRepository.findByEmail("user-test-member@gmail.com")
                        .orElseThrow()
        );

        attachment.setDropboxFileId("dropbox-id-new");
        attachment.setFilename("new-file.pdf");
        attachment.setUploadDate(LocalDateTime.now());

        Attachment saved = attachmentRepository.save(attachment);

        assertThat(saved.getId())
                .isNotNull();

        assertThat(attachmentRepository.findById(saved.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("Find all by task id - existing task - returns only its attachments")
    @Sql(
            scripts = {
                    "classpath:database/clean-data.sql",
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql",
                    "classpath:database/tasks/add-repository-tasks.sql",
                    "classpath:database/attachments/add-attachment-repository-attachments.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:database/clean-data.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByTaskId_ExistingTaskId_ReturnsAttachmentsForThatTask() {

        List<Attachment> taskOneAttachments =
                attachmentRepository.findAllByTaskId(1L);

        assertThat(taskOneAttachments)
                .hasSize(2);

        assertThat(taskOneAttachments)
                .extracting(Attachment::getFilename)
                .containsExactlyInAnyOrder(
                        "design.pdf",
                        "notes.txt"
                );

        List<Attachment> taskTwoAttachments =
                attachmentRepository.findAllByTaskId(2L);

        assertThat(taskTwoAttachments)
                .hasSize(1);

        assertThat(taskTwoAttachments)
                .extracting(Attachment::getFilename)
                .containsExactly("screenshot.png");
    }

    @Test
    @DisplayName("Find all by task id - nonexistent task - returns empty list")
    @Sql(
            scripts = {
                    "classpath:database/clean-data.sql",
                    "classpath:database/users/insert-user-test-users.sql",
                    "classpath:database/projects/insert-repository-test-projects.sql",
                    "classpath:database/tasks/add-repository-tasks.sql",
                    "classpath:database/attachments/add-attachment-repository-attachments.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:database/clean-data.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void findAllByTaskId_NonexistentTaskId_ReturnsEmptyList() {

        List<Attachment> actual =
                attachmentRepository.findAllByTaskId(999L);

        assertThat(actual)
                .isEmpty();
    }
}