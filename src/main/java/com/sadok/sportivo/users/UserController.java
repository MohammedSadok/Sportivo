package com.sadok.sportivo.users;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sadok.sportivo.users.dto.CreateUserRequest;
import com.sadok.sportivo.users.dto.UpdateCredentialsRequest;
import com.sadok.sportivo.users.dto.UpdateUserRequest;
import com.sadok.sportivo.users.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('admin')")
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.createUser(request);
  }

  @GetMapping
  @PreAuthorize("hasRole('admin')")
  public List<UserResponse> getAllUsers() {
    return userService.getAllUsers();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('admin')")
  public UserResponse getUserById(@PathVariable UUID id) {
    return userService.getUserById(id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('admin')")
  public UserResponse updateUserByAdmin(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRequest request) {
    return userService.updateUser(id, request);
  }

  @PatchMapping("/{id}/credentials")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<Void> updateCredentials(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCredentialsRequest request) {
    userService.updateCredentials(id, request);
    return ResponseEntity.noContent().build();
  }

  /** DELETE /api/v1/users/{id} — admin deletes a user */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  /** GET /api/v1/users/me — authenticated user reads own profile */
  @GetMapping("/me")
  @PreAuthorize("hasAnyRole('user', 'admin')")
  public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
    UUID id = UUID.fromString(jwt.getSubject());
    return userService.getUserById(id);
  }

  /** PUT /api/v1/users/me — authenticated user updates own profile */
  @PutMapping("/me")
  @PreAuthorize("hasAnyRole('user', 'admin')")
  public UserResponse updateMe(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateUserRequest request) {
    UUID id = UUID.fromString(jwt.getSubject());
    return userService.updateUser(id, request);
  }
}
