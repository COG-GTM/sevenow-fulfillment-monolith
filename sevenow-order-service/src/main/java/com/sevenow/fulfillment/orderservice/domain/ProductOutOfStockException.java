package com.sevenow.fulfillment.orderservice.domain;

import java.util.List;

/**
 * Thrown when an ordered product has no on-hand quantity at the store and no configured substitute.
 */
public class ProductOutOfStockException extends RuntimeException {

  private final List<String> rejectedProductIds;

  public ProductOutOfStockException(List<String> rejectedProductIds) {
    super("Products out of stock with no substitute: " + rejectedProductIds);
    this.rejectedProductIds = rejectedProductIds;
  }

  public List<String> getRejectedProductIds() {
    return rejectedProductIds;
  }
}
