package com.sevenow.delivery.web;

import com.sevenow.delivery.common.Address;

import java.time.LocalDateTime;

/**
 * Body of {@code POST /deliveries}; also the shape of the monolith's OrderAccepted payload
 * ({@code eventType} is optional and only checked when present).
 */
public class ScheduleDeliveryRequest {

  private String eventType;
  private Long orderId;
  private LocalDateTime readyBy;
  private Address deliveryAddress;

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public LocalDateTime getReadyBy() {
    return readyBy;
  }

  public void setReadyBy(LocalDateTime readyBy) {
    this.readyBy = readyBy;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(Address deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }
}
