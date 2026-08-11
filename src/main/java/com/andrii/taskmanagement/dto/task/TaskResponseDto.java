package com.andrii.taskmanagement.dto.task;

import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskResponseDto {
    private Long id;
    private Long projectId;
    private Long assigneeId;
    private Long createdById;
    private String name;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<Long> labelIds;
}
