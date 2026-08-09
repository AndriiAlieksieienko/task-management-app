package com.andrii.taskmanagement.validation.date;

import com.andrii.taskmanagement.dto.project.ProjectDates;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProjectDatesValidator
        implements ConstraintValidator<ValidProjectDates, ProjectDates> {

    @Override
    public boolean isValid(ProjectDates dto,
                           ConstraintValidatorContext context) {

        if (dto.startDate() == null || dto.endDate() == null) {
            return true;
        }

        return !dto.endDate().isBefore(dto.startDate());
    }
}
