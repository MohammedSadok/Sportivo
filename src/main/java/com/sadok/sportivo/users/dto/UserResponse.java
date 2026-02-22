package com.sadok.sportivo.users.dto;

import com.sadok.sportivo.users.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model returned to callers. Never exposes credentials or internal
 * details.
 */
public record UserResponse(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    UserRole role,
    Instant createdAt,
    Instant updatedAt) {
}
