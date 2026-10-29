package com.sevenow.inventory.domain;

public class ReservationNotFoundException extends RuntimeException {

  private final long reservationId;

  public ReservationNotFoundException(long reservationId) {
    super("reservation not found: " + reservationId);
    this.reservationId = reservationId;
  }

  public long getReservationId() {
    return reservationId;
  }
}
