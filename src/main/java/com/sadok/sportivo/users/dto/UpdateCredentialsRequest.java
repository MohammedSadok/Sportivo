package com.sadok.sportivo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin-only request to reset a user's password in Keycloak.
 */
public record UpdateCredentialsRequest(
                @NotBlank(message = "{validation.password.notBlank}") @Size(min = 8, message = "{validation.password.size}") String newPassword) {
}
