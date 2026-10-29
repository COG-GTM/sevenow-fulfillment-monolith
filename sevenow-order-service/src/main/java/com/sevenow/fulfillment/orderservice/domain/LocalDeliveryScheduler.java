package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.domain.Action;
import com.sevenow.fulfillment.domain.Driver;
import com.sevenow.fulfillment.domain.DriverRepository;
import com.sevenow.fulfillment.domain.Order;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;

/** Pre-extraction behaviour: schedules against the monolith's own drivers/driver_actions tables. */
public class LocalDeliveryScheduler implements DeliveryScheduler {

  private final DriverRepository driverRepository;
  private final Random random = new Random();

  public LocalDeliveryScheduler(DriverRepository driverRepository) {
    this.driverRepository = driverRepository;
  }

  @Override
  public long scheduleDelivery(Order order, LocalDateTime readyBy) {
    List<Driver> drivers = driverRepository.findAllAvailable();
    if (drivers.isEmpty()) {
      throw new IllegalStateException("no available driver for order " + order.getId());
    }
    Driver driver = drivers.get(random.nextInt(drivers.size()));
    driver.addAction(Action.makePickup(order));
    driver.addAction(Action.makeDropoff(order, readyBy.plusMinutes(30)));
    return driver.getId();
  }

  @Override
  public List<DeliveryAction> actionsFor(long driverId, long orderId) {
    return driverRepository.findById(driverId)
            .map(driver -> driver.getPlan().getActions().stream()
                    .filter(a -> a.actionFor(orderId))
                    .map(a -> new DeliveryAction(a.getType().name(), a.getTime()))
                    .collect(toList()))
            .orElse(Collections.emptyList());
  }
}
