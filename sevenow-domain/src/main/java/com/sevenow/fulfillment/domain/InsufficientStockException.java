package com.sevenow.fulfillment.domain;

import java.util.List;

/**
 * Thrown when the quantity ordered of a product exceeds the store's on-hand quantity.
 */
public class InsufficientStockException extends RuntimeException {

  private final List<String> productIds;

  public InsufficientStockException(List<String> productIds) {
    super("Requested quantity exceeds on-hand stock for products: " + productIds);
    this.productIds = productIds;
  }

  public List<String> getProductIds() {
    return productIds;
  }
}
