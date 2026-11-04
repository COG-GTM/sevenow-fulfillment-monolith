package com.sevenow.fulfillment.domain;

/**
 * Thrown when an order's line items, or a revision of them, are malformed:
 * no line items, a quantity below 1, or a revision naming a product the order does not contain.
 */
public class InvalidOrderLineItemsException extends RuntimeException {
  public InvalidOrderLineItemsException(String message) {
    super(message);
  }
}
