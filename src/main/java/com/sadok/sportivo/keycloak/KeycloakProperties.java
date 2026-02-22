package com.sadok.sportivo.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(
    String serverUrl,
    String realm,
    String clientId,
    String clientSecret) {
}
