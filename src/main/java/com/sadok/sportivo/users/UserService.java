package com.sadok.sportivo.users;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sadok.sportivo.common.exception.ResourceAlreadyExistsException;
import com.sadok.sportivo.common.exception.ResourceNotFoundException;
import com.sadok.sportivo.keycloak.KeycloakAdminService;
import com.sadok.sportivo.keycloak.KeycloakAdminService.UserCreationResult;
import com.sadok.sportivo.mail.MailService;
import com.sadok.sportivo.users.dto.CreateUserRequest;
import com.sadok.sportivo.users.dto.UpdateCredentialsRequest;
import com.sadok.sportivo.users.dto.UpdateUserRequest;
import com.sadok.sportivo.users.dto.UserResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final KeycloakAdminService keycloakAdminService;
  private final UserMapper userMapper;
  private final MailService mailService;

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    validateUniqueness(request.username(), request.email());
    UserCreationResult result = keycloakAdminService.createUser(request);
    UUID keycloakId = result.keycloakId();
    try {
      User user = User.builder()
          .id(keycloakId)
          .username(request.username())
          .email(request.email())
          .firstName(request.firstName())
          .lastName(request.lastName())
          .role(request.role())
          .build();

      User saved = userRepository.save(user);
      log.info("User created locally [id={}, username={}]", saved.getId(), saved.getUsername());

      mailService.sendWelcomeEmail(request.email(), request.username(), result.generatedPassword());

      return userMapper.toResponse(saved);

    } catch (Exception ex) {
      log.error("Local persistence failed for user [keycloakId={}]; rolling back Keycloak user", keycloakId, ex);
      keycloakAdminService.deleteUser(keycloakId);
      throw ex;
    }
  }

  // -------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------

  /** Admin-only: list all users. */
  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(userMapper::toResponse)
        .toList();
  }

  /**
   * Returns the user with the given ID (admin can read any, user can only read
   * own).
   */
  @Transactional(readOnly = true)
  public UserResponse getUserById(UUID id) {
    return userMapper.toResponse(findByIdOrThrow(id));
  }

  // -------------------------------------------------------------------------
  // Update profile
  // -------------------------------------------------------------------------

  /**
   * Updates the user's profile in both Keycloak and locally.
   * Only non-null fields in the request are applied (partial update / PATCH
   * semantics).
   */
  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    User user = findByIdOrThrow(id);

    keycloakAdminService.updateUser(id, request.email(), request.firstName(), request.lastName());
    userMapper.updateUserFromRequest(request, user);

    User saved = userRepository.save(user);
    log.info("User updated [id={}]", saved.getId());
    return userMapper.toResponse(saved);
  }

  // -------------------------------------------------------------------------
  // Update credentials
  // -------------------------------------------------------------------------

  /** Admin-only: resets the user's Keycloak password. */
  public void updateCredentials(UUID id, UpdateCredentialsRequest request) {
    // Ensure the user exists locally before touching Keycloak
    findByIdOrThrow(id);
    keycloakAdminService.resetPassword(id, request.newPassword());
    log.info("Credentials updated for user [id={}]", id);
  }

  // -------------------------------------------------------------------------
  // Delete
  // -------------------------------------------------------------------------

  /** Admin-only: deletes the user from local DB and Keycloak. */
  @Transactional
  public void deleteUser(UUID id) {
    User user = findByIdOrThrow(id);
    userRepository.delete(user);
    keycloakAdminService.deleteUser(id);
    log.info("User deleted [id={}]", id);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private User findByIdOrThrow(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found [id=" + id + "]"));
  }

  private void validateUniqueness(String username, String email) {
    if (userRepository.existsByUsername(username)) {
      throw new ResourceAlreadyExistsException("Username already taken: " + username);
    }
    if (userRepository.existsByEmail(email)) {
      throw new ResourceAlreadyExistsException("Email already registered: " + email);
    }
  }
}
