package com.andrii.taskmanagement.repository.label;

import com.andrii.taskmanagement.model.Label;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findAllByProjectId(Long projectId);
}
