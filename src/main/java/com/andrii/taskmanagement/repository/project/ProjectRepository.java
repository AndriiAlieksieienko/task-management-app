package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByProjectManagerId(
            Long projectManagerId,
            Pageable pageable
    );

    @Query("""
            SELECT pm.project
            FROM ProjectMember pm
            WHERE pm.user.id = :userId
            """)
    Page<Project> findProjectsByMemberId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
