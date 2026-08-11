package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectManagerUpdateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectSearchParameters;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.ProjectMapper;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import com.andrii.taskmanagement.service.ProjectService;
import com.andrii.taskmanagement.specification.ProjectSpecificationBuilder;
import com.andrii.taskmanagement.specification.ProjectSpecifications;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final ProjectSpecificationBuilder projectSpecificationBuilder;
    private final ProjectAccessService projectAccessService;

    @Override
    public ProjectResponseDto save(
            ProjectCreateRequestDto requestDto,
            String creatorEmail
    ) {
        User creator = findUserByEmail(creatorEmail);

        User projectManager = userRepository.findByIdAndRoleName(
                        requestDto.projectManagerId(),
                        RoleName.ROLE_PROJECT_MANAGER
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Project manager not found by id: "
                                + requestDto.projectManagerId()
                ));

        Project project = projectMapper.toModel(requestDto);

        project.setCreatedBy(creator);
        project.setProjectManager(projectManager);
        project.setStatus(ProjectStatus.INITIATED);

        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        Project savedProject = projectRepository.save(project);

        return projectMapper.toProjectResponse(savedProject, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> findAll(
            ProjectSearchParameters searchParameters,
            Pageable pageable,
            String email
    ) {
        User user = findUserByEmail(email);

        Specification<Project> specification = projectSpecificationBuilder.build(searchParameters);

        if (isProjectManager(user)) {
            specification = specification.and(
                    ProjectSpecifications.hasProjectManager(user.getId())
            );
        } else if (isTeamMember(user)) {
            specification = specification.and(
                    ProjectSpecifications.hasMember(user.getId())
            );
        }

        Page<Project> projectPage = projectRepository.findAll(specification, pageable);
        List<Project> projects = projectPage.getContent();

        if (projects.isEmpty()) {
            return projectPage.map(project ->
                    projectMapper.toProjectResponse(project, List.of())
            );
        }

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

        List<ProjectMember> projectMembers = projectMemberRepository
                        .findAllByProjectIdsWithUsers(projectIds);

        Map<Long, List<ProjectMemberResponseDto>> membersByProject =
                projectMembers.stream()
                        .collect(Collectors.groupingBy(
                                pm -> pm.getProject().getId(),
                                Collectors.mapping(
                                        projectMapper::toProjectMemberResponse,
                                        Collectors.toList()
                                )
                        ));

        return projectPage.map(project ->
                projectMapper.toProjectResponse(
                        project,
                        membersByProject.getOrDefault(
                                project.getId(),
                                List.of()
                        )
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDto findById(Long id, String email) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        projectAccessService.checkCanViewProject(user, project);

        List<ProjectMemberResponseDto> members = findProjectMembers(project.getId());

        return projectMapper.toProjectResponse(project, members);
    }

    @Override
    public ProjectResponseDto updateById(
            Long id,
            ProjectUpdateRequestDto requestDto,
            String email
    ) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        projectAccessService.checkCanModifyProject(user, project);

        projectMapper.updateProjectFromDto(requestDto, project);

        project.setUpdatedAt(LocalDateTime.now());

        Project updatedProject = projectRepository.save(project);

        List<ProjectMemberResponseDto> members = findProjectMembers(project.getId());

        return projectMapper.toProjectResponse(updatedProject, members);
    }

    @Override
    public void deleteById(Long id, String email) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        projectAccessService.checkCanModifyProject(user, project);

        projectRepository.delete(project);
    }

    @Override
    public ProjectResponseDto updateProjectManager(
            Long projectId,
            ProjectManagerUpdateRequestDto requestDto
    ) {
        Project project = findProjectById(projectId);

        User projectManager = userRepository
                .findByIdAndRoleName(
                        requestDto.projectManagerId(),
                        RoleName.ROLE_PROJECT_MANAGER
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Project manager not found by id: "
                                + requestDto.projectManagerId()
                ));

        project.setProjectManager(projectManager);
        project.setUpdatedAt(LocalDateTime.now());

        Project updatedProject = projectRepository.save(project);

        List<ProjectMemberResponseDto> members = findProjectMembers(projectId);

        return projectMapper.toProjectResponse(updatedProject, members);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found by email: " + email
                ));
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find the project by id: " + id
                ));
    }

    private boolean isProjectManager(User user) {
        return user.getRole().getName() == RoleName.ROLE_PROJECT_MANAGER;
    }

    private boolean isTeamMember(User user) {
        return user.getRole().getName() == RoleName.ROLE_TEAM_MEMBER;
    }

    private List<ProjectMemberResponseDto> findProjectMembers(Long projectId) {
        return projectMemberRepository
                .findAllByProjectIdsWithUsers(List.of(projectId))
                .stream()
                .map(projectMapper::toProjectMemberResponse)
                .toList();
    }
}
