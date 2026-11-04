package com.sevenow.fulfillment.domain;

public class OrderMinimumNotMetException extends RuntimeException {
  public OrderMinimumNotMetException() {
    super("Order total is below the order minimum");
  }
}
