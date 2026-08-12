package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.dto.attachment.AttachmentResponseDto;
import com.andrii.taskmanagement.model.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfiguration.class)
public interface AttachmentMapper {
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "uploadedById", source = "uploadedBy.id")
    AttachmentResponseDto toDto(Attachment attachment);

    @Mapping(target = "task", ignore = true)
    Attachment toModel(AttachmentCreateRequestDto requestDto);
}
