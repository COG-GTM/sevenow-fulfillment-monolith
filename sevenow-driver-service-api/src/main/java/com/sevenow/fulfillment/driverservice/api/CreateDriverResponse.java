package com.sevenow.fulfillment.driverservice.api;

public class CreateDriverResponse {
  private long id;

  public CreateDriverResponse() {
  }

  public CreateDriverResponse(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
