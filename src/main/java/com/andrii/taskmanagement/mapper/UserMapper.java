package com.andrii.taskmanagement.mapper;

import com.andrii.taskmanagement.config.MapperConfiguration;
import com.andrii.taskmanagement.dto.user.UserRegistrationRequestDto;
import com.andrii.taskmanagement.dto.user.UserResponseDto;
import com.andrii.taskmanagement.dto.user.UserUpdateRequestDto;
import com.andrii.taskmanagement.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfiguration.class)
public interface UserMapper {
    @Mapping(source = "role.name", target = "role")
    UserResponseDto toUserResponse(User user);

    User toModel(UserRegistrationRequestDto requestDto);

    void updateUserFromDto(
            UserUpdateRequestDto dto,
            @MappingTarget User user
    );
}
