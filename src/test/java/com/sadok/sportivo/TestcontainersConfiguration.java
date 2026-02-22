package com.sadok.sportivo;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Base testcontainers configuration.
 * <p>
 * PostgreSQL is replaced by H2 (in-memory, PostgreSQL-compatibility mode) for
 * fast test execution.
 * Add container beans here only when a real external service is required in
 * tests.
 * For Keycloak integration tests see
 * {@code KeycloakTestcontainersConfiguration}.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    // no containers – H2 handles persistence in tests
}
