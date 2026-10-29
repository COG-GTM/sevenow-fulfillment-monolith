package com.sevenow.delivery.domain;

public class DriverNotFoundException extends RuntimeException {

  private final long driverId;

  public DriverNotFoundException(long driverId) {
    super("driver not found: " + driverId);
    this.driverId = driverId;
  }

  public long getDriverId() {
    return driverId;
  }
}
