package com.andrii.taskmanagement.repository.attachment;

import com.andrii.taskmanagement.model.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findAllByTaskId(Long taskId);
}
