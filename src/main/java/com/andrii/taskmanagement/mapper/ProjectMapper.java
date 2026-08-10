package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfiguration.class)
public interface ProjectMapper {
    @Mapping(source = "project.createdBy.id", target = "createdById")
    @Mapping(source = "project.projectManager.id", target = "projectManagerId")
    @Mapping(source = "members", target = "members")
    ProjectResponseDto toProjectResponse(
            Project project,
            List<ProjectMemberResponseDto> members
    );

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "joinedAt", target = "joinedAt")
    ProjectMemberResponseDto toProjectMemberResponse(
            ProjectMember projectMember
    );

    Project toModel(ProjectCreateRequestDto requestDto);

    void updateProjectFromDto(
            ProjectUpdateRequestDto dto,
            @MappingTarget Project project
    );
}
