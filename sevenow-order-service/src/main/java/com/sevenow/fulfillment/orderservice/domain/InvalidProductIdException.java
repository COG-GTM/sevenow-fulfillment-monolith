package com.sevenow.fulfillment.orderservice.domain;

public class InvalidProductIdException extends RuntimeException {
  public InvalidProductIdException(String productId) {
    super("Invalid product id " + productId);
  }
}
