package com.andrii.taskmanagement.repository.attachment;

import com.andrii.taskmanagement.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
