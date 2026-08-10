package com.andrii.taskmanagement.repository.project;

import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.user
            WHERE pm.project.id IN :projectIds
            """)
    List<ProjectMember> findAllByProjectIdsWithUsers(
            @Param("projectIds") Collection<Long> projectIds
    );

    List<ProjectMember> findAllByIdProjectId(Long projectId);

    void deleteByIdProjectIdAndIdUserId(Long projectId, Long userId);
}
