package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessServiceImpl implements ProjectAccessService {
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void checkCanViewProject(User user, Project project
    ) {
        if (isAdmin(user)) {
            return;
        }

        if (isProjectManager(user)
                && project.getProjectManager()
                .getId()
                .equals(user.getId())) {
            return;
        }

        if (isTeamMember(user) && projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(),
                user.getId()
        )) {
            return;
        }

        throw new AccessDeniedException("You don't have access to this project");
    }

    @Override
    public void checkCanModifyProject(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }

        if (isProjectManager(user)
                && project.getProjectManager()
                .getId()
                .equals(user.getId())) {
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
