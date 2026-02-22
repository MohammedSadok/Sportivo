package com.sadok.sportivo.common.exception;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found");
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  ProblemDetail handleConflict(ResourceAlreadyExistsException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage(), "already-exists");
  }

  @ExceptionHandler(KeycloakException.class)
  ProblemDetail handleKeycloak(KeycloakException ex) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "keycloak-error");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            f -> f.getField(),
            f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
            (a, b) -> a));
    ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "validation-error");
    pd.setProperty("errors", errors);
    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "validation-error");
  }

  private ProblemDetail problem(HttpStatus status, String detail, String errorCode) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create("https://sportivo.local/errors/" + errorCode));
    return pd;
  }
}
