package com.andrii.taskmanagement.dto.project;

import com.andrii.taskmanagement.model.ProjectStatus;
import java.time.LocalDate;

public record ProjectSearchParameters(
        String name,
        ProjectStatus status,
        Long projectManagerId,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        LocalDate endDateFrom,
        LocalDate endDateTo
) {
}
