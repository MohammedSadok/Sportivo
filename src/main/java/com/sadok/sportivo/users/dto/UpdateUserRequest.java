package com.sadok.sportivo.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update a user's own profile (or admin updating any user).
 * All fields are optional — only non-null values are applied.
 */
public record UpdateUserRequest(

        @Size(max = 100, message = "{validation.firstName.size}") String firstName,

        @Size(max = 100, message = "{validation.lastName.size}") String lastName,

        @Email(message = "{validation.email.invalid}") @Size(max = 255, message = "{validation.email.size}") String email) {
}
