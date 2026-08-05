package com.andrii.taskmanagement.dto.user;

import com.andrii.taskmanagement.validation.field.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FieldMatch(
        first = "password",
        second = "repeatPassword",
        message = "Passwords don't match."
)
public class UserRegistrationRequestDto {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    @Size(min = 8, max = 20)
    @NotBlank
    private String repeatPassword;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}
