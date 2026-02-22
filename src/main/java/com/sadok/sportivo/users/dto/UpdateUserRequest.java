package com.sadok.sportivo.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update a user's own profile (or admin updating any user).
 * All fields are optional — only non-null values are applied.
 */
public record UpdateUserRequest(

    @Size(max = 100) String firstName,

    @Size(max = 100) String lastName,

    @Email @Size(max = 255) String email) {
}
