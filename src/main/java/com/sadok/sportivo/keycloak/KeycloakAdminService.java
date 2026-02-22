package com.sadok.sportivo.keycloak;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.sadok.sportivo.common.exception.KeycloakException;
import com.sadok.sportivo.users.UserRole;
import com.sadok.sportivo.users.dto.CreateUserRequest;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

  public record UserCreationResult(UUID keycloakId, String generatedPassword) {
  }

  private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
  private static final int PASSWORD_LENGTH = 12;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final Keycloak keycloak;
  private final KeycloakProperties keycloakProperties;

  public UserCreationResult createUser(CreateUserRequest request) {
    String generatedPassword = generatePassword();
    UserRepresentation user = buildUserRepresentation(request, generatedPassword);

    try (Response response = realm().users().create(user)) {
      if (response.getStatus() != 201) {
        String body = response.hasEntity() ? response.readEntity(String.class) : "no body";
        throw new KeycloakException(
            "Failed to create user in Keycloak [status=%d]: %s".formatted(response.getStatus(), body));
      }

      String location = response.getHeaderString("Location");
      String keycloakId = location.substring(location.lastIndexOf('/') + 1);
      UUID userId = UUID.fromString(keycloakId);

      assignRealmRole(userId, request.role());
      return new UserCreationResult(userId, generatedPassword);
    } catch (KeycloakException ke) {
      throw ke;
    } catch (Exception ex) {
      throw new KeycloakException("Unexpected error while creating user in Keycloak", ex);
    }
  }

  public void updateUser(UUID userId, String email, String firstName, String lastName) {
    try {
      UserResource userResource = realm().users().get(userId.toString());
      UserRepresentation rep = userResource.toRepresentation();
      if (email != null)
        rep.setEmail(email);
      if (firstName != null)
        rep.setFirstName(firstName);
      if (lastName != null)
        rep.setLastName(lastName);
      userResource.update(rep);
    } catch (Exception ex) {
      throw new KeycloakException("Failed to update user in Keycloak [id=" + userId + "]", ex);
    }
  }

  public void resetPassword(UUID userId, String newPassword) {
    try {
      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(newPassword);
      credential.setTemporary(false);
      realm().users().get(userId.toString()).resetPassword(credential);
    } catch (Exception ex) {
      throw new KeycloakException("Failed to reset password in Keycloak [id=" + userId + "]", ex);
    }
  }

  public void deleteUser(UUID userId) {
    try {
      realm().users().delete(userId.toString());
    } catch (Exception ex) {
    }
  }

  private RealmResource realm() {
    return keycloak.realm(keycloakProperties.realm());
  }

  private String generatePassword() {
    StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }

  private UserRepresentation buildUserRepresentation(CreateUserRequest request, String password) {
    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(true);

    UserRepresentation user = new UserRepresentation();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEnabled(true);
    user.setEmailVerified(true);
    user.setCredentials(List.of(credential));
    return user;
  }

  private void assignRealmRole(UUID userId, UserRole role) {
    RoleRepresentation realmRole = realm().roles().get(role.name().toLowerCase()).toRepresentation();
    realm().users().get(userId.toString()).roles().realmLevel().add(List.of(realmRole));
  }
}
