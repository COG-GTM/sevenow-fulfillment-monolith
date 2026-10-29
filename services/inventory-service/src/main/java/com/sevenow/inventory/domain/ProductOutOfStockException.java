package com.sevenow.inventory.domain;

import java.util.List;

public class ProductOutOfStockException extends RuntimeException {

  private final List<String> rejectedProductIds;

  public ProductOutOfStockException(List<String> rejectedProductIds) {
    super("out of stock with no in-stock substitute: " + rejectedProductIds);
    this.rejectedProductIds = rejectedProductIds;
  }

  public List<String> getRejectedProductIds() {
    return rejectedProductIds;
  }
}
