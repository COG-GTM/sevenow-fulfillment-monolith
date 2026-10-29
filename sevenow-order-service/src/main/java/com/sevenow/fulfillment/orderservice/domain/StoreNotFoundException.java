package com.sevenow.fulfillment.orderservice.domain;

public class StoreNotFoundException extends RuntimeException {
  public StoreNotFoundException(long storeId) {
    super("Store not found with id " + storeId);
  }
}
