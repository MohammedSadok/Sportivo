# Stage 1 - Build the application
# Use the full JDK image to build the application. Alpine = smaller base
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy ONLY the Gradle wrapper and dependency descriptors first.
# Docker caches this layer. If build.gradle doesn't change, `./gradlew dependencies`
# won't re-download the internet on every build — saving 30–90 seconds.

COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make the wrapper executable and pre-download dependencies into the layer cache.
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# This layer invalidate only when the actual code change
COPY src ./src

# Build the fat JAE, skip tests we run them in CI
RUN ./gradlew bootJar --no-daemon -x test


# Stage 2 - RUNTIME
# Use JRE (not jdk) - no compiler, no javac , no shell
FROM eclipse-temurin:21-jre-alpine

# --- Security: non-root user ---
# Running as root inside a container is dangerous. If the app is compromised,
# the attacker gets root inside the container and can potentially escape.
# Alpine uses `addgroup`/`adduser` (BusyBox) syntax.

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# --- Copy ONLY the built JAR from the builder stage ---
# No source code, no Gradle wrapper, no .git, no build scripts in the final image.
COPY --from=builder /build/build/libs/*.jar app.jar

# Lock down file permissions: the appuser owns the JAR but cannot write to it.
RUN chown appuser:appgroup app.jar && chmod 440 app.jar

# Switch to the non-root user
USER appuser

# --- Expose the application port ---
EXPOSE 8080

# --- Health check ---
# Docker and Kubernetes use this to know if the container is healthy.
# /actuator/health is provided by spring-boot-starter-actuator.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
 
# --- Entrypoint with production JVM flags ---
# -XX:+UseContainerSupport    : JVM respects cgroup CPU/memory limits (Java 11+)
# -XX:MaxRAMPercentage=75.0   : Use 75% of the container's memory limit for heap
# -Djava.security.egd=...     : Faster startup — use /dev/urandom for entropy
# -Dspring.profiles.active    : Override with -e SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]