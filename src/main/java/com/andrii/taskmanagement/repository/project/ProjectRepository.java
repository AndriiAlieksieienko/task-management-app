package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
