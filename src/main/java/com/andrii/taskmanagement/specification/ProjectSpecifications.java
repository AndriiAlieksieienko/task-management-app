package com.andrii.taskmanagement.specification;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ProjectSpecifications {
    private ProjectSpecifications() {
    }

    public static Specification<Project> nameContains(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Project> hasProjectManager(Long projectManagerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("projectManager").get("id"), projectManagerId);
    }

    public static Specification<Project> hasMember(Long userId) {
        return (root, query, criteriaBuilder) -> {
            Subquery<Long> subquery = query.subquery(Long.class);

            Root<ProjectMember> projectMember = subquery.from(ProjectMember.class);

            subquery.select(projectMember.get("project").get("id"));
            subquery.where(
                    criteriaBuilder.equal(projectMember.get("user").get("id"), userId),
                    criteriaBuilder.equal(projectMember.get("project").get("id"), root.get("id")
                    )
            );

            return criteriaBuilder.exists(subquery);
        };
    }

    public static Specification<Project> startDateFrom(LocalDate date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Project> startDateTo(LocalDate date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Project> endDateFrom(LocalDate date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), date);
    }

    public static Specification<Project> endDateTo(LocalDate date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), date);
    }
}
