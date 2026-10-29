package com.sevenow.fulfillment.domain;

/**
 * Lifecycle of a 7NOW delivery order.
 *
 * APPROVED -> ACCEPTED -> PICKING -> PACKED -> READY_FOR_PICKUP -> PICKED_UP -> DELIVERED
 * APPROVED -> CANCELLED
 */
public enum OrderState {
  APPROVED,
  ACCEPTED,
  PICKING,
  PACKED,
  READY_FOR_PICKUP,
  PICKED_UP,
  DELIVERED,
  CANCELLED,
}
