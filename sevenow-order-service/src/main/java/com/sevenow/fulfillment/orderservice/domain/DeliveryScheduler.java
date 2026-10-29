package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.domain.Order;

import java.time.LocalDateTime;
import java.util.List;

/** Assigns an accepted order to a driver. Driver/plan data is owned by delivery-service. */
public interface DeliveryScheduler {

  /** @return the assigned driver id */
  long scheduleDelivery(Order order, LocalDateTime readyBy);

  List<DeliveryAction> actionsFor(long driverId, long orderId);
}
