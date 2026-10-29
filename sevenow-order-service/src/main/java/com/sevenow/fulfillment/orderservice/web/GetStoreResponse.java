package com.sevenow.fulfillment.orderservice.web;

public class GetStoreResponse {
  private long storeId;

  private GetStoreResponse() {
  }

  public GetStoreResponse(long storeId) {

    this.storeId = storeId;
  }

  public long getStoreId() {
    return storeId;
  }

  public void setStoreId(long storeId) {
    this.storeId = storeId;
  }
}
