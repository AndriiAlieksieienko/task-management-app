package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelResponseDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.LabelMapper;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.label.LabelRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.LabelService;
import com.andrii.taskmanagement.service.ProjectAccessService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final LabelMapper labelMapper;
    private final ProjectAccessService projectAccessService;

    @Override
    @Transactional
    public LabelResponseDto save(
            LabelCreateRequestDto requestDto,
            String userEmail
    ) {
        User user = getUserByEmail(userEmail);
        Project project = getProjectById(requestDto.projectId());

        projectAccessService.checkCanModifyProject(user, project);

        Label label = labelMapper.toModel(requestDto);
        label.setProject(project);
        label.setCreatedAt(LocalDateTime.now());

        Label savedLabel = labelRepository.save(label);

        return labelMapper.toDto(savedLabel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponseDto> findAll(Long projectId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Project project = getProjectById(projectId);

        projectAccessService.checkCanViewProject(user, project);

        return labelRepository.findAllByProjectId(projectId)
                .stream()
                .map(labelMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public LabelResponseDto updateById(
            Long id,
            LabelUpdateRequestDto requestDto,
            String userEmail
    ) {
        User user = getUserByEmail(userEmail);
        Label label = getLabelById(id);

        projectAccessService.checkCanModifyProject(user, label.getProject());

        labelMapper.updateLabelFromDto(requestDto, label);

        return labelMapper.toDto(labelRepository.save(label));
    }

    @Override
    @Transactional
    public void deleteById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Label label = getLabelById(id);

        projectAccessService.checkCanModifyProject(user, label.getProject());

        labelRepository.delete(label);
    }

    private Label getLabelById(Long id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Label not found with id: " + id
                ));
    }

    private Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Project not found with id: " + id
                ));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with email: " + email
                ));
    }
}
