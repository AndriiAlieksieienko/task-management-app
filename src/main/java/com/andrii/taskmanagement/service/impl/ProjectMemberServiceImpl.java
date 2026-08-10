package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.project.ProjectMemberCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.ProjectMapper;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import com.andrii.taskmanagement.service.ProjectMemberService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccessService;

    @Override
    public ProjectMemberResponseDto addMember(
            Long projectId,
            ProjectMemberCreateRequestDto requestDto,
            String email
    ) {
        User currentUser = findUserByEmail(email);
        Project project = findProjectById(projectId);

        projectAccessService.checkCanModifyProject(currentUser, project);

        User user = userRepository.findByIdAndRoleName(
                requestDto.userId(),
                RoleName.ROLE_TEAM_MEMBER
        ).orElseThrow(() -> new EntityNotFoundException(
                "Team member not found by id: " + requestDto.userId()
        ));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new IllegalStateException("User is already a member of this project");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, user.getId());

        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(projectMemberId);
        projectMember.setProject(project);
        projectMember.setUser(user);
        projectMember.setJoinedAt(LocalDateTime.now());

        ProjectMember savedMember = projectMemberRepository.save(projectMember);

        return projectMapper.toProjectMemberResponse(savedMember);
    }

    @Override
    public void removeMember(Long projectId, Long userId, String email) {
        User currentUser = findUserByEmail(email);
        Project project = findProjectById(projectId);

        projectAccessService.checkCanModifyProject(currentUser, project);

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new EntityNotFoundException("User is not a member of this project");
        }

        projectMemberRepository.deleteByIdProjectIdAndIdUserId(projectId, userId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found by email: " + email
                ));
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find the project by id: " + projectId
                ));
    }
}

