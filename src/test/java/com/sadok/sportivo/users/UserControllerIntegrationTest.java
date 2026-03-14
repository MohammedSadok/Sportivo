package com.sadok.sportivo.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sadok.sportivo.KeycloakTestcontainersConfiguration;
import com.sadok.sportivo.users.dto.CreateUserRequest;
import com.sadok.sportivo.users.dto.UpdateCredentialsRequest;
import com.sadok.sportivo.users.dto.UpdateUserRequest;
import com.sadok.sportivo.users.dto.UserResponse;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Full-stack integration tests for {@link UserController}.
 *
 * <ul>
 * <li>H2 in-memory DB (PostgreSQL mode) via {@code application-test.yml} —
 * fast, no Docker for DB</li>
 * <li>Real Keycloak container with the sportivo realm imported from test
 * resources</li>
 * <li>Real JWTs obtained from Keycloak via direct-access grants (password
 * flow)</li>
 * <li>MockMvc manually constructed from WebApplicationContext</li>
 * </ul>
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(KeycloakTestcontainersConfiguration.class)
@DisplayName("UserController — integration")
class UserControllerIntegrationTest {

  @Autowired
  WebApplicationContext context;
  @Autowired
  UserRepository userRepository;
  @MockitoBean
  JwtDecoder jwtDecoder;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @DynamicPropertySource
  static void keycloakProps(DynamicPropertyRegistry registry) {
    KeycloakTestcontainersConfiguration.overrideKeycloakProperties(registry);
  }

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
    objectMapper = new ObjectMapper();
    given(jwtDecoder.decode(anyString())).willAnswer(inv -> decodeJwtWithoutVerification(inv.getArgument(0)));
  }

  @AfterEach
  void cleanDb() {
    userRepository.deleteAll();
  }

  // -------------------------------------------------------------------------
  // Token helpers
  // -------------------------------------------------------------------------

  private String adminToken() {
    return tokenFor("test-admin", "AdminPass1!");
  }

  private String userToken() {
    return tokenFor("test-user", "UserPass1!");
  }

  private String tokenFor(String username, String password) {
    KeycloakContainer kc = KeycloakTestcontainersConfiguration.KEYCLOAK;
    try (Keycloak kClient = KeycloakBuilder.builder()
        .serverUrl(kc.getAuthServerUrl())
        .realm("sportivo")
        .clientId("sportivo-client-api")
        .clientSecret("dhovr7EeG4FKxzvuj7sVfg6oHqhZuW72")
        .username(username)
        .password(password)
        .grantType("password")
        .build()) {
      String token = kClient.tokenManager().getAccessTokenString();
      if (token == null || token.trim().isEmpty()) {
        throw new RuntimeException("Token is null or empty for user: " + username);
      }
      return token;
    } catch (Exception ex) {
      throw new RuntimeException("Failed to get token for user: " + username + " - " + ex.getMessage(), ex);
    }
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  // -------------------------------------------------------------------------
  // POST /api/v1/users
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("admin can create a user")
  void adminCreatesUser() throws Exception {
    CreateUserRequest req = new CreateUserRequest(
        "newplayer", "newplayer@sportivo.test", "New", "Player", UserRole.USER);

    mockMvc.perform(post("/api/v1/users")
        .header("Authorization", bearer(adminToken()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username", is("newplayer")))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  @DisplayName("regular user cannot create another user (403)")
  void regularUserCannotCreateUser() throws Exception {
    CreateUserRequest req = new CreateUserRequest(
        "hacker", "hacker@sportivo.test", "H", "K", UserRole.USER);

    mockMvc.perform(post("/api/v1/users")
        .header("Authorization", bearer(userToken()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("unauthenticated request is rejected with 401")
  void unauthenticatedReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized());
  }

  // -------------------------------------------------------------------------
  // GET /api/v1/users
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("admin can list all users")
  void adminListsUsers() throws Exception {
    mockMvc.perform(get("/api/v1/users")
        .header("Authorization", bearer(adminToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  // -------------------------------------------------------------------------
  // GET /api/v1/users/me
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("user can read own profile via /me")
  void userReadsOwnProfile() throws Exception {
    String jwt = userToken();
    UUID keycloakId = subjectFromToken(jwt);

    userRepository.save(new User(
        keycloakId,
        "test-user",
        "test-user@sportivo.test",
        "Test",
        "User",
        UserRole.USER));

    mockMvc.perform(get("/api/v1/users/me")
        .header("Authorization", bearer(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("test-user")));
  }

  // -------------------------------------------------------------------------
  // PATCH /api/v1/users/{id}/credentials
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("admin can reset a user's credentials")
  void adminResetsCredentials() throws Exception {
    CreateUserRequest create = new CreateUserRequest(
        "credsplayer", "credsplayer@sportivo.test", "Creds", "Player", UserRole.USER);
    String body = mockMvc.perform(post("/api/v1/users")
        .header("Authorization", bearer(adminToken()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID id = objectMapper.readValue(body, UserResponse.class).id();

    mockMvc.perform(patch("/api/v1/users/" + id + "/credentials")
        .header("Authorization", bearer(adminToken()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new UpdateCredentialsRequest("NewPass123!"))))
        .andExpect(status().isNoContent());
  }

  // -------------------------------------------------------------------------
  // DELETE /api/v1/users/{id}
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("admin can delete a user")
  void adminDeletesUser() throws Exception {
    CreateUserRequest create = new CreateUserRequest(
        "tobedeleted", "tobedeleted@sportivo.test", "Del", "Me", UserRole.USER);
    String body = mockMvc.perform(post("/api/v1/users")
        .header("Authorization", bearer(adminToken()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID id = objectMapper.readValue(body, UserResponse.class).id();

    mockMvc.perform(delete("/api/v1/users/" + id)
        .header("Authorization", bearer(adminToken())))
        .andExpect(status().isNoContent());

    assertThat(userRepository.findById(id)).isEmpty();
  }

  // -------------------------------------------------------------------------
  // PUT /api/v1/users/me
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("user can update own profile")
  void userUpdatesOwnProfile() throws Exception {
    String jwt = userToken();
    UUID keycloakId = subjectFromToken(jwt);

    User saved = userRepository.save(new User(
        keycloakId,
        "test-user",
        "test-user@sportivo.test",
        "Test",
        "User",
        UserRole.USER));
    userRepository.save(saved);

    mockMvc.perform(put("/api/v1/users/me")
        .header("Authorization", bearer(jwt))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(
            new UpdateUserRequest("UpdatedFirst", "UpdatedLast", null))));

    mockMvc.perform(put("/api/v1/users/me")
        .header("Authorization", bearer(jwt))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(
            new UpdateUserRequest("UpdatedFirst", "UpdatedLast", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is("UpdatedFirst")));
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Decodes the JWT subject (Keycloak user ID) without signature verification.
   */
  private static UUID subjectFromToken(String token) {
    String[] parts = token.split("\\.");
    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
    String sub = payload.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
    return UUID.fromString(sub);
  }

  @SuppressWarnings("unchecked")
  private Jwt decodeJwtWithoutVerification(String token) {
    try {
      if (token == null || token.trim().isEmpty()) {
        throw new RuntimeException("Token is null or empty");
      }
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new RuntimeException("Token has " + parts.length + " parts, expected 3. Token: " + token);
      }
      String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
      });

      long issuedAtEpoch = ((Number) claims.getOrDefault("iat", Instant.now().getEpochSecond())).longValue();
      long expiresAtEpoch = ((Number) claims.getOrDefault("exp", Instant.now().plusSeconds(3600).getEpochSecond()))
          .longValue();

      return Jwt.withTokenValue(token)
          .header("alg", "none")
          .issuedAt(Instant.ofEpochSecond(issuedAtEpoch))
          .expiresAt(Instant.ofEpochSecond(expiresAtEpoch))
          .subject((String) claims.get("sub"))
          .claims(c -> c.putAll(claims))
          .build();
    } catch (Exception ex) {
      throw new IllegalArgumentException("Unable to decode JWT in integration test: " + ex.getMessage(), ex);
    }
  }
}
