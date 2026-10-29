package com.sevenow.delivery.web;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.domain.Action;

import java.util.List;

public class ScheduleDeliveryResponse {

  private final long orderId;
  private final long driverId;
  private final Address deliveryAddress;
  private final List<Action> actions;

  public ScheduleDeliveryResponse(long orderId, long driverId, Address deliveryAddress, List<Action> actions) {
    this.orderId = orderId;
    this.driverId = driverId;
    this.deliveryAddress = deliveryAddress;
    this.actions = actions;
  }

  public long getOrderId() {
    return orderId;
  }

  public long getDriverId() {
    return driverId;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public List<Action> getActions() {
    return actions;
  }
}
