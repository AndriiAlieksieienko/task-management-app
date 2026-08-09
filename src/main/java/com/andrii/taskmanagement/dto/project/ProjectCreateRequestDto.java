package com.andrii.taskmanagement.dto.project;

import com.andrii.taskmanagement.validation.date.ValidProjectDates;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@ValidProjectDates
public record ProjectCreateRequestDto(
        @NotBlank
        String name,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull
        Long projectManagerId
) implements ProjectDates {
}
