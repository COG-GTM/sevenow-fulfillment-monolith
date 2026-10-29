package com.sevenow.fulfillment.driverservice.api;

public class DriverAvailability {

  private boolean available;

  public DriverAvailability() {
  }

  public DriverAvailability(boolean available) {
    this.available = available;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }
}
