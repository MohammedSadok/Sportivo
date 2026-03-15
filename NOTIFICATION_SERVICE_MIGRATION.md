# Notification Service Migration Guide

This document outlines the step-by-step instructions and best practices for decoupling the email/management functionality from the main `sportivo` monolithic application into a dedicated, standalone **Notification Service** using a Pub/Sub integration architecture (RabbitMQ).

## 1. Architectural Changes

- **Current State**: The `sportivo` application handles user management and synchronously or internally produces/processes emails using `com.sadok.sportivo.mail.*` components.
- **Future State**:
  - `sportivo` (Main App) acts as a **Producer**. It publishes domain events (e.g., `UserCreatedEvent`) to a RabbitMQ Topic Exchange. It no longer sends emails itself.
  - `notification-service` acts as a **Consumer**. It subscribes to the RabbitMQ queue, consumes these events, and triggers the actual email sending via `MailService`.

## 2. Steps to Create the Notification Service

### Step 2.1: Initialize the New Spring Boot Project

Create a new Spring Boot application (e.g., via Spring Initializr) named `notification-service`.
**Dependencies needed:**

- Spring Boot Starter Mail (`spring-boot-starter-mail`)
- Spring Boot Starter AMQP (`spring-boot-starter-amqp`)
- Spring Boot Starter Web (Optional, useful for health checks/Prometheus metrics)
- Lombok

### Step 2.2: Migrate the Source Code

Move the associated email configurations and services from `sportivo` to `notification-service`:

1. **Remove** the `com.sadok.sportivo.mail` package from the `sportivo` app.
2. **Copy** `MailService.java`, `MailConfig.java`, and `MailProperties.java` into the `notification-service` codebase.
3. Migrate any Email templates (e.g., HTML/Thymeleaf templates or plain text resource files) to the `src/main/resources` folder of the new service.

### Step 2.3: Define Shared Event Payloads

Create a standardized DTO/Record that acts as the contract for your RabbitMQ messages. Keep these models simple and serialization-friendly (JSON).
_Example:_

```java
public record UserCreatedEvent(
    Long userId,
    String email,
    String firstName,
    String lastName,
    String language // "en" or "fr"
) {}
```

_(Recommendation: Consider extracting shared formats into a common library, or explicitly duplicate the DTOs in both projects mapping to the same JSON structure)._

### Step 2.4: Configure RabbitMQ Producer (in `sportivo` main app)

- Keep/Update `RabbitMqProducer.java` in the main application.
- Ensure the main app only sends messages to the exchange using routing keys (e.g., `sportivo.notifications.email.welcome`).
- Send the `UserCreatedEvent` payload converted to JSON.

### Step 2.5: Configure RabbitMQ Consumer (in `notification-service`)

Create a message listener to consume the RabbitMQ events:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final MailService mailService;

    @RabbitListener(queues = "${rabbitmq.queue.notification}")
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for email: {}", event.email());
        // Call your mail service here
        mailService.sendWelcomeEmail(event.email(), event.firstName());
    }
}
```

### Step 2.6: Application Configuration (`application.yml`)

Move SMTP and email properties from `sportivo`'s configuration over to the new service.

**Notification Service `application.yml`**:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  mail:
    host: smtp.yourprovider.com
    port: 587
    username: your_username
    password: your_password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

rabbitmq:
  exchange: sportivo.exchange
  queue:
    notification: sportivo.queue
  routing:
    key: sportivo.notifications.#
```

---

## 3. Best Practices to Follow

### Use JSON Serialization

Configure a `Jackson2JsonMessageConverter` for RabbitMQ in both applications so that message payloads are correctly mapped from objects into JSON and back.

```java
@Bean
public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
}
```

### Idempotency

Ensure the `notification-service` is **idempotent**. Network issues or RabbitMQ delivery retries might cause the same event to be consumed twice. Use a unique message ID or track processed events in a small database/cache if strict exactly-once email delivery is required.

### Dead Letter Queues (DLQ)

Configure a Dead Letter Exchange (DLX) and Dead Letter Queue (DLQ). If `MailService` throws an exception (e.g., SMTP server is down), the message should be retried automatically (using Spring AMQP retry properties) and eventually routed to the DLQ if it consistently fails. This prevents data loss and allows you to inspect failing messages later.

### Correlation IDs (Tracing)

Pass a Correlation ID or Trace ID across your async message headers (e.g., using Micrometer / Spring Cloud Sleuth/Zipkin). This helps trace a user creation request from the main `sportivo` API directly down into the `notification-service` logs.

### Asynchronous & Non-Blocking

Do not block the AMQP listener threads directly waiting for a slow SMTP connection. Consider tweaking concurrency settings (`spring.rabbitmq.listener.simple.concurrency`) in `notification-service` so consuming high volume notification bursts doesn't bottleneck.

### Alerting & Monitoring

Integrate Promtail/Loki (as you already use it) to monitor logs for the new service uniquely. Track failed email dispatches using metrics (Micrometer/Grafana) to quickly detect SMTP configuration errors or rejected domains.
