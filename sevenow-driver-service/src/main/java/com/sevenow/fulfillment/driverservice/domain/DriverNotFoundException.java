package com.sevenow.fulfillment.driverservice.domain;

public class DriverNotFoundException extends RuntimeException {
  public DriverNotFoundException(long driverId) {
    super("Driver not found: " + driverId);
  }
}
