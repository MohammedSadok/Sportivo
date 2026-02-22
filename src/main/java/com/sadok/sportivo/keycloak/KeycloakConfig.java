package com.sadok.sportivo.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

  @Bean
  public Keycloak keycloakAdminClient(KeycloakProperties props) {
    return KeycloakBuilder.builder()
        .serverUrl(props.serverUrl())
        .realm(props.realm())
        .clientId(props.clientId())
        .clientSecret(props.clientSecret())
        .grantType("client_credentials")
        .build();
  }
}
