package com.sadok.sportivo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Shared Keycloak testcontainer configuration for integration tests.
 *
 * <p>
 * Uses a static container so it is started once and reused across all test
 * classes
 * that import this configuration, keeping total test time short.
 * The realm is imported from {@code classpath:keycloak/sportivo-realm.json}.
 * </p>
 *
 * <p>
 * Usage — annotate your test class with:
 * </p>
 * 
 * <pre>{@code
 * &#64;Import(KeycloakTestcontainersConfiguration.class)
 * }</pre>
 * 
 * and call {@link #overrideKeycloakProperties} from a
 * {@code @DynamicPropertySource} method.
 */
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakTestcontainersConfiguration {

  public static final KeycloakContainer KEYCLOAK = new KeycloakContainer(
      DockerImageName.parse("quay.io/keycloak/keycloak:latest").toString())
      .withRealmImportFile("/keycloak/sportivo-realm.json");

  static {
    KEYCLOAK.start();
  }

  @Bean
  public KeycloakContainer keycloakContainer() {
    return KEYCLOAK;
  }

  /**
   * Registers Keycloak-related Spring properties so {@code @SpringBootTest} tests
   * pick up the Testcontainer URLs automatically.
   *
   * <p>
   * Call this from a {@code @DynamicPropertySource} static method in your test
   * class:
   * </p>
   * 
   * <pre>{@code
   * @DynamicPropertySource
   * static void keycloakProps(DynamicPropertyRegistry registry) {
   *   KeycloakTestcontainersConfiguration.overrideKeycloakProperties(registry);
   * }
   * }</pre>
   */
  public static void overrideKeycloakProperties(DynamicPropertyRegistry registry) {
    String authServerUrl = KEYCLOAK.getAuthServerUrl();
    String realm = "sportivo";

    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> authServerUrl + "realms/" + realm);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> authServerUrl + "realms/" + realm + "/protocol/openid-connect/certs");

    registry.add("app.keycloak.server-url", () -> authServerUrl);
    registry.add("app.keycloak.realm", () -> realm);
    registry.add("app.keycloak.client-id", () -> "sportivo-client-api");
    registry.add("app.keycloak.client-secret", () -> "dhovr7EeG4FKxzvuj7sVfg6oHqhZuW72");
  }
}
