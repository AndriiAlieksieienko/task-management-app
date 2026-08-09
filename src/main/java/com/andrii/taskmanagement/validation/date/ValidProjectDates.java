package com.andrii.taskmanagement.validation.date;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProjectDatesValidator.class)
@Documented
public @interface ValidProjectDates {

    String message() default "End date must not be before start date.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
