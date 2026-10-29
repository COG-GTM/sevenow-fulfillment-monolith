package com.sevenow.delivery.web;

import com.sevenow.delivery.domain.DriverNotFoundException;
import com.sevenow.delivery.domain.NoDriverAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class DeliveryExceptionHandler {

  @ExceptionHandler(DriverNotFoundException.class)
  public ResponseEntity<Void> notFound() {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(NoDriverAvailableException.class)
  public ResponseEntity<Map<String, Object>> noDriver(NoDriverAvailableException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
  }
}
