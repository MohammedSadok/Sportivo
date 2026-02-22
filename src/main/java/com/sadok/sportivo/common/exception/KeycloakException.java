package com.sadok.sportivo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KeycloakException extends RuntimeException {

  public KeycloakException(String message, Throwable cause) {
    super(message, cause);
  }

  public KeycloakException(String message) {
    super(message);
  }
}
