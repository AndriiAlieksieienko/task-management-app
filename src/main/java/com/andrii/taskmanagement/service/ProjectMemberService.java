package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.project.ProjectMemberCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;

public interface ProjectMemberService {
    ProjectMemberResponseDto addMember(
            Long projectId,
            ProjectMemberCreateRequestDto requestDto,
            String memberEmail
    );

    void removeMember(Long projectId, Long userId, String userEmail);
}
