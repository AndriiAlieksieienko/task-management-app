package com.andrii.taskmanagement.specification;

import com.andrii.taskmanagement.dto.project.ProjectSearchParameters;
import com.andrii.taskmanagement.model.Project;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProjectSpecificationBuilder {

    public Specification<Project> build(
            ProjectSearchParameters searchParameters
    ) {
        Specification<Project> specification = Specification.where(null);

        if (searchParameters.name() != null && !searchParameters.name().isBlank()) {
            specification = specification.and(
                    ProjectSpecifications.nameContains(searchParameters.name())
            );
        }

        if (searchParameters.status() != null) {
            specification = specification.and(
                    ProjectSpecifications.hasStatus(searchParameters.status())
            );
        }

        if (searchParameters.projectManagerId() != null) {
            specification = specification.and(
                    ProjectSpecifications.hasProjectManager(searchParameters.projectManagerId())
            );
        }

        if (searchParameters.startDateFrom() != null) {
            specification = specification.and(
                    ProjectSpecifications.startDateFrom(searchParameters.startDateFrom())
            );
        }

        if (searchParameters.startDateTo() != null) {
            specification = specification.and(
                    ProjectSpecifications.startDateTo(searchParameters.startDateTo())
            );
        }

        if (searchParameters.endDateFrom() != null) {
            specification = specification.and(
                    ProjectSpecifications.endDateFrom(searchParameters.endDateFrom())
            );
        }

        if (searchParameters.endDateTo() != null) {
            specification = specification.and(
                    ProjectSpecifications.endDateTo(searchParameters.endDateTo())
            );
        }

        return specification;
    }
}
