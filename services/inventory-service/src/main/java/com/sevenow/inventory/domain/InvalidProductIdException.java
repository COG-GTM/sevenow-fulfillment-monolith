package com.sevenow.inventory.domain;

public class InvalidProductIdException extends RuntimeException {

  private final String productId;

  public InvalidProductIdException(String productId) {
    super("invalid product id: " + productId);
    this.productId = productId;
  }

  public String getProductId() {
    return productId;
  }
}
