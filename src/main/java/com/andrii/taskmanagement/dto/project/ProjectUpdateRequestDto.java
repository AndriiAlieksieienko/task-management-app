package com.andrii.taskmanagement.dto.project;

import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.validation.date.ValidProjectDates;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@ValidProjectDates
public record ProjectUpdateRequestDto(
        @NotBlank
        String name,

        String description,

        ProjectStatus status,

        LocalDate startDate,

        LocalDate endDate
) implements ProjectDates {
}
