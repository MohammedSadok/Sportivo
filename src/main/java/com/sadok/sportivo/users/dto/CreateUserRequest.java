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

        @NotBlank @Size(min = 3, max = 100) String username,

        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotNull UserRole role) {
}
