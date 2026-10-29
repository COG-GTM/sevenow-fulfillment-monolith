package com.sevenow.delivery.domain;

public class NoDriverAvailableException extends RuntimeException {

  private final long orderId;

  public NoDriverAvailableException(long orderId) {
    super("no available driver for order " + orderId);
    this.orderId = orderId;
  }

  public long getOrderId() {
    return orderId;
  }
}
