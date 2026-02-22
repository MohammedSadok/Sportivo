package com.sadok.sportivo.users.dto;

import com.sadok.sportivo.users.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload used by admins to create a new user.
 * The password is automatically generated and sent to the user by email.
 */

public record CreateUserRequest(

                @NotBlank(message = "{validation.username.notBlank}") @Size(min = 3, max = 100, message = "{validation.username.size}") String username,

                @NotBlank(message = "{validation.email.notBlank}") @Email(message = "{validation.email.invalid}") @Size(max = 255, message = "{validation.email.size}") String email,

                @NotBlank(message = "{validation.firstName.notBlank}") @Size(max = 100, message = "{validation.firstName.size}") String firstName,

                @NotBlank(message = "{validation.lastName.notBlank}") @Size(max = 100, message = "{validation.lastName.size}") String lastName,

                @NotNull(message = "{validation.role.notNull}") UserRole role) {
}
