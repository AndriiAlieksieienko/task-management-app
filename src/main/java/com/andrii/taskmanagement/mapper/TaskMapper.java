package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskResponseDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Task;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfiguration.class)
public interface TaskMapper {
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(target = "labelIds", ignore = true)
    TaskResponseDto toTaskResponse(Task task);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "labels", ignore = true)
    Task toModel(TaskCreateRequestDto requestDto);

    @Mapping(target = "labels", ignore = true)
    void updateTaskFromDto(TaskUpdateRequestDto requestDto, @MappingTarget Task task);

    @Mapping(target = "labels", ignore = true)
    void updateTaskFromAssignedDto(
            TaskAssignedUpdateRequestDto requestDto,
            @MappingTarget Task task
    );

    @AfterMapping
    default void setLabelIds(@MappingTarget TaskResponseDto taskResponseDto, Task task) {
        Set<Long> labelIds = task.getLabels() == null
                ? new HashSet<>()
                : task.getLabels().stream()
                .map(Label::getId)
                .collect(Collectors.toSet());

        taskResponseDto.setLabelIds(labelIds);
    }
}
