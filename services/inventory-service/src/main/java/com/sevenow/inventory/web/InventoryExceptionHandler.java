package com.sevenow.inventory.web;

import com.sevenow.inventory.domain.InvalidProductIdException;
import com.sevenow.inventory.domain.ProductOutOfStockException;
import com.sevenow.inventory.domain.ReservationNotFoundException;
import com.sevenow.inventory.domain.StoreNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class InventoryExceptionHandler {

  /** Same {"rejectedProductIds":[...]} body as the monolith's order endpoint, but 409 Conflict. */
  @ExceptionHandler(ProductOutOfStockException.class)
  public ResponseEntity<Map<String, List<String>>> outOfStock(ProductOutOfStockException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("rejectedProductIds", e.getRejectedProductIds()));
  }

  @ExceptionHandler(InvalidProductIdException.class)
  public ResponseEntity<Map<String, String>> invalidProduct(InvalidProductIdException e) {
    return ResponseEntity.badRequest().body(Collections.singletonMap("invalidProductId", e.getProductId()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
  }

  @ExceptionHandler({StoreNotFoundException.class, ReservationNotFoundException.class})
  public ResponseEntity<Void> notFound() {
    return ResponseEntity.notFound().build();
  }
}
