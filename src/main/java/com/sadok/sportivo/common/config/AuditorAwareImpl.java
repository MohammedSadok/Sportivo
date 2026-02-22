package com.sadok.sportivo.common.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<UUID> {

  @Override
  public Optional<UUID> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication instanceof JwtAuthenticationToken)) {
      return Optional.empty();
    }

    Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
    String subject = jwt.getSubject();

    try {
      return Optional.of(UUID.fromString(subject));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
