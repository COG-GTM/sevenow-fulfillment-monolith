package com.sevenow.delivery.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/** Delivery scheduling, moved here from the monolith's {@code OrderService.scheduleDelivery}. */
@Service
@Transactional
public class DeliveryService {

  private final DriverRepository driverRepository;
  private final Random random;

  @Autowired
  public DeliveryService(DriverRepository driverRepository) {
    this(driverRepository, new Random());
  }

  DeliveryService(DriverRepository driverRepository, Random random) {
    this.driverRepository = driverRepository;
    this.random = random;
  }

  /**
   * Assigns the order to a random available driver and appends its PICKUP and DROPOFF
   * (readyBy + 30 min) actions. Idempotent per orderId: if the order is already scheduled the
   * existing driver is returned, so the monolith can safely retry after a failed transaction. The
   * {@code delivery_assignments} claim is the first statement of the transaction, so concurrent calls for one
   * order serialize on its primary key and the loser sees the winner's committed actions.
   */
  public Driver schedule(long orderId, LocalDateTime readyBy) {
    if (driverRepository.claimOrder(orderId) == 0) {
      List<Driver> alreadyAssigned = driverRepository.findByOrderId(orderId);
      if (alreadyAssigned.isEmpty()) {
        throw new IllegalStateException("order " + orderId + " is claimed but has no driver actions");
      }
      return alreadyAssigned.get(0);
    }
    List<Driver> drivers = driverRepository.findAllAvailable();
    if (drivers.isEmpty()) {
      throw new NoDriverAvailableException(orderId);
    }
    Driver driver = drivers.get(random.nextInt(drivers.size()));
    driver.addAction(Action.makePickup(orderId));
    driver.addAction(Action.makeDropoff(orderId, readyBy.plusMinutes(30)));
    driverRepository.assignOrder(orderId, driver.getId());
    return driver;
  }
}
