package com.sevenow.delivery.web;

import com.sevenow.delivery.domain.DeliveryService;
import com.sevenow.delivery.domain.Driver;
import org.springframework.web.bind.annotation.*;

@RestController
public class DeliveryController {

  private final DeliveryService deliveryService;

  public DeliveryController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  @RequestMapping(path = "/deliveries", method = RequestMethod.POST)
  public ScheduleDeliveryResponse schedule(@RequestBody ScheduleDeliveryRequest request) {
    if (request.getEventType() != null && !"OrderAccepted".equals(request.getEventType())) {
      throw new IllegalArgumentException("unsupported eventType: " + request.getEventType());
    }
    if (request.getOrderId() == null || request.getReadyBy() == null) {
      throw new IllegalArgumentException("orderId and readyBy are required");
    }
    long orderId = request.getOrderId();
    Driver driver = deliveryService.schedule(orderId, request.getReadyBy());
    return new ScheduleDeliveryResponse(orderId, driver.getId(), request.getDeliveryAddress(), driver.actionsForDelivery(orderId));
  }
}
