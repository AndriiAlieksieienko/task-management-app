package com.andrii.taskmanagement.repository.task;

import com.andrii.taskmanagement.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAllByProjectId(Long projectId, Pageable pageable);
}
