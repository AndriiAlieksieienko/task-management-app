package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelResponseDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import com.andrii.taskmanagement.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfiguration.class)
public interface LabelMapper {

    @Mapping(source = "project.id", target = "projectId")
    LabelResponseDto toDto(Label label);

    @Mapping(target = "project", ignore = true)
    Label toModel(LabelCreateRequestDto requestDto);

    void updateLabelFromDto(
            LabelUpdateRequestDto dto,
            @MappingTarget Label label
    );
}
