package com.andrii.taskmanagement.repository.label;

import com.andrii.taskmanagement.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {
}
