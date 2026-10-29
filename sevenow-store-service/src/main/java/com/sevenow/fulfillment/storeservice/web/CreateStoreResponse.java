package com.sevenow.fulfillment.storeservice.web;

public class CreateStoreResponse {
  private long id;

  public CreateStoreResponse() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public CreateStoreResponse(long id) {
    this.id = id;
  }
}
