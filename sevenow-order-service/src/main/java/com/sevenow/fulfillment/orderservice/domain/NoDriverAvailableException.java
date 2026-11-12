package com.sevenow.fulfillment.orderservice.domain;

public class NoDriverAvailableException extends RuntimeException {
  public NoDriverAvailableException() {
    super("No approved driver is available to deliver this order");
  }
}
