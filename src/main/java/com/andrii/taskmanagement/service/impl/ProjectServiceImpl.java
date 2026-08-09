package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectManagerUpdateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.ProjectMapper;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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

    @Override
    public ProjectResponseDto save(
            ProjectCreateRequestDto requestDto,
            String creatorEmail
    ) {
        User creator = findUserByEmail(creatorEmail);

        User projectManager = userRepository
                .findByIdAndRoleName(
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

        return projectMapper.toProjectResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> findAll(
            Pageable pageable,
            String email
    ) {
        User user = findUserByEmail(email);

        Page<Project> projects;
        if (isAdmin(user)) {
            projects = projectRepository.findAll(pageable);

        } else if (isProjectManager(user)) {
            projects = projectRepository.findByProjectManagerId(
                    user.getId(),
                    pageable
            );

        } else {
            projects = projectRepository.findProjectsByMemberId(
                    user.getId(),
                    pageable
            );
        }

        return projects.map(projectMapper::toProjectResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDto findById(
            Long id,
            String email
    ) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        checkCanViewProject(user, project);

        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponseDto updateById(
            Long id,
            ProjectUpdateRequestDto requestDto,
            String email
    ) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        checkCanModifyProject(user, project);

        projectMapper.updateProjectFromDto(requestDto, project);
        project.setUpdatedAt(LocalDateTime.now());

        return projectMapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    public void deleteById(
            Long id,
            String email
    ) {
        User user = findUserByEmail(email);
        Project project = findProjectById(id);

        checkCanModifyProject(user, project);

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

        return projectMapper.toProjectResponse(updatedProject);
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

    private void checkCanViewProject(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }

        if (isProjectManager(user)) {
            if (project.getProjectManager().getId().equals(user.getId())) {
                return;
            }

            throw new AccessDeniedException("You don't have access to this project");
        }

        if (isTeamMember(user)) {
            boolean member = projectMemberRepository
                    .existsByProjectIdAndUserId(
                            project.getId(),
                            user.getId()
                    );

            if (member) {
                return;
            }

            throw new AccessDeniedException("You don't have access to this project");
        }

        throw new AccessDeniedException("You don't have permission to access projects");
    }

    private void checkCanModifyProject(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }

        if (isProjectManager(user)
                && project.getProjectManager().getId().equals(user.getId())) {
            return;
        }

        throw new AccessDeniedException("You don't have permission to modify this project");
    }

    private boolean isAdmin(User user) {
        return user.getRole().getName() == RoleName.ROLE_ADMIN;
    }

    private boolean isProjectManager(User user) {
        return user.getRole().getName() == RoleName.ROLE_PROJECT_MANAGER;
    }

    private boolean isTeamMember(User user) {
        return user.getRole().getName() == RoleName.ROLE_TEAM_MEMBER;
    }
}
