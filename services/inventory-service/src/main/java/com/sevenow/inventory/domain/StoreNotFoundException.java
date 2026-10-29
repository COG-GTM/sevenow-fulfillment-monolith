package com.sevenow.inventory.domain;

public class StoreNotFoundException extends RuntimeException {

  private final long storeId;

  public StoreNotFoundException(long storeId) {
    super("store not found: " + storeId);
    this.storeId = storeId;
  }

  public long getStoreId() {
    return storeId;
  }
}
