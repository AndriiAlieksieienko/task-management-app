package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.User;

public interface ProjectAccessService {
    void checkCanViewProject(User user, Project project);

    void checkCanModifyProject(User user, Project project);
}
