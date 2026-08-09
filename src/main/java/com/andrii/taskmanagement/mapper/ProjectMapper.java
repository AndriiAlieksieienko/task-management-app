package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfiguration.class)
public interface ProjectMapper {

    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "projectManager.id", target = "projectManagerId")
    ProjectResponseDto toProjectResponse(Project project);

    Project toModel(ProjectCreateRequestDto requestDto);

    void updateProjectFromDto(
            ProjectUpdateRequestDto dto,
            @MappingTarget Project project
    );
}
