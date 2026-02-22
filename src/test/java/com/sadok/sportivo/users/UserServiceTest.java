package com.sadok.sportivo.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sadok.sportivo.common.exception.ResourceAlreadyExistsException;
import com.sadok.sportivo.common.exception.ResourceNotFoundException;
import com.sadok.sportivo.keycloak.KeycloakAdminService;
import com.sadok.sportivo.keycloak.KeycloakAdminService.UserCreationResult;
import com.sadok.sportivo.mail.MailService;
import com.sadok.sportivo.users.dto.CreateUserRequest;
import com.sadok.sportivo.users.dto.UpdateCredentialsRequest;
import com.sadok.sportivo.users.dto.UpdateUserRequest;
import com.sadok.sportivo.users.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

  @Mock
  UserRepository userRepository;
  @Mock
  KeycloakAdminService keycloakAdminService;
  @Mock
  UserMapper userMapper;
  @Mock
  MailService mailService;

  @InjectMocks
  UserService userService;

  private static final UUID USER_ID = UUID.randomUUID();

  private User sampleUser;
  private UserResponse sampleResponse;

  @BeforeEach
  void setUp() {
    sampleUser = User.builder()
        .id(USER_ID)
        .username("alice")
        .email("alice@example.com")
        .firstName("Alice")
        .lastName("Smith")
        .role(UserRole.USER)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    sampleResponse = new UserResponse(
        USER_ID, "alice", "alice@example.com", "Alice", "Smith",
        UserRole.USER, sampleUser.getCreatedAt(), sampleUser.getUpdatedAt());
  }

  // -------------------------------------------------------------------------
  // createUser
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("createUser")
  class CreateUser {

    private CreateUserRequest validRequest() {
      return new CreateUserRequest(
          "alice", "alice@example.com", "Alice", "Smith", UserRole.USER);
    }

    @Test
    @DisplayName("creates user in Keycloak then locally and returns response")
    void happyPath() {
      given(userRepository.existsByUsername("alice")).willReturn(false);
      given(userRepository.existsByEmail("alice@example.com")).willReturn(false);
      given(keycloakAdminService.createUser(any())).willReturn(new UserCreationResult(USER_ID, "TmpPwd123!"));
      given(userRepository.save(any())).willReturn(sampleUser);
      given(userMapper.toResponse(sampleUser)).willReturn(sampleResponse);

      UserResponse result = userService.createUser(validRequest());

      assertThat(result).isEqualTo(sampleResponse);
      then(keycloakAdminService).should().createUser(any());
      then(userRepository).should().save(any());
    }

    @Test
    @DisplayName("throws ResourceAlreadyExistsException when username is taken")
    void duplicateUsername() {
      given(userRepository.existsByUsername("alice")).willReturn(true);

      assertThatThrownBy(() -> userService.createUser(validRequest()))
          .isInstanceOf(ResourceAlreadyExistsException.class)
          .hasMessageContaining("alice");

      then(keycloakAdminService).should(never()).createUser(any());
    }

    @Test
    @DisplayName("throws ResourceAlreadyExistsException when email is taken")
    void duplicateEmail() {
      given(userRepository.existsByUsername("alice")).willReturn(false);
      given(userRepository.existsByEmail("alice@example.com")).willReturn(true);

      assertThatThrownBy(() -> userService.createUser(validRequest()))
          .isInstanceOf(ResourceAlreadyExistsException.class)
          .hasMessageContaining("alice@example.com");
    }

    @Test
    @DisplayName("deletes Keycloak user (compensating tx) when local save fails")
    void rollsBackKeycloakWhenLocalSaveFails() {
      given(userRepository.existsByUsername("alice")).willReturn(false);
      given(userRepository.existsByEmail("alice@example.com")).willReturn(false);
      given(keycloakAdminService.createUser(any())).willReturn(new UserCreationResult(USER_ID, "TmpPwd123!"));
      given(userRepository.save(any())).willThrow(new RuntimeException("DB error"));

      assertThatThrownBy(() -> userService.createUser(validRequest()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("DB error");

      then(keycloakAdminService).should().deleteUser(USER_ID);
    }
  }

  // -------------------------------------------------------------------------
  // getAllUsers
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getAllUsers returns mapped list")
  void getAllUsers() {
    given(userRepository.findAll()).willReturn(List.of(sampleUser));
    given(userMapper.toResponse(sampleUser)).willReturn(sampleResponse);

    List<UserResponse> result = userService.getAllUsers();

    assertThat(result).containsExactly(sampleResponse);
  }

  // -------------------------------------------------------------------------
  // getUserById
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getUserById returns response for existing user")
  void getUserById_found() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));
    given(userMapper.toResponse(sampleUser)).willReturn(sampleResponse);

    assertThat(userService.getUserById(USER_ID)).isEqualTo(sampleResponse);
  }

  @Test
  @DisplayName("getUserById throws ResourceNotFoundException for unknown ID")
  void getUserById_notFound() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // updateUser
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("updateUser delegates to Keycloak and persists changes")
  void updateUser() {
    UpdateUserRequest req = new UpdateUserRequest("NewFirst", "NewLast", "new@example.com");
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));
    given(userRepository.save(sampleUser)).willReturn(sampleUser);
    given(userMapper.toResponse(sampleUser)).willReturn(sampleResponse);

    userService.updateUser(USER_ID, req);

    then(keycloakAdminService).should().updateUser(USER_ID, "new@example.com", "NewFirst", "NewLast");
    then(userMapper).should().updateUserFromRequest(req, sampleUser);
    then(userRepository).should().save(sampleUser);
  }

  // -------------------------------------------------------------------------
  // updateCredentials
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("updateCredentials delegates to KeycloakAdminService")
  void updateCredentials() {
    UpdateCredentialsRequest req = new UpdateCredentialsRequest("NewPass123!");
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));

    userService.updateCredentials(USER_ID, req);

    then(keycloakAdminService).should().resetPassword(USER_ID, "NewPass123!");
  }

  @Test
  @DisplayName("updateCredentials throws when user not found locally")
  void updateCredentials_userNotFound() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateCredentials(USER_ID, new UpdateCredentialsRequest("pass")))
        .isInstanceOf(ResourceNotFoundException.class);

    then(keycloakAdminService).should(never()).resetPassword(any(), any());
  }

  // -------------------------------------------------------------------------
  // deleteUser
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("deleteUser removes from DB and Keycloak")
  void deleteUser() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));

    userService.deleteUser(USER_ID);

    then(userRepository).should().delete(sampleUser);
    then(keycloakAdminService).should().deleteUser(USER_ID);
  }
}
