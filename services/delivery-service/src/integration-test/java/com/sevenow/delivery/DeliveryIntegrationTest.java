package com.sevenow.delivery;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;
import com.sevenow.delivery.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Runs against the real sevenow_delivery schema (Flyway-migrated on startup). */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DeliveryIntegrationTest {

  @Autowired
  private DriverService driverService;

  @Autowired
  private DeliveryService deliveryService;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  public void seedsDriversOneAndTwo() {
    assertTrue(driverService.findDriverById(1L).isAvailable());
    assertFalse(driverService.findDriverById(2L).isAvailable());
  }

  @Test
  public void shouldPersistPickupAndDropoffForOrderIdAndBeIdempotent() {
    Driver created = driverService.createDriver(new PersonName("Ivy", "Integration"), new Address("1 Main St", null, "Irving", "TX", "75063"));
    driverService.updateAvailability(created.getId(), true);
    long orderId = System.nanoTime() % 1_000_000_000L;
    LocalDateTime readyBy = LocalDateTime.of(2026, 10, 29, 12, 0);

    long driverId = deliveryService.schedule(orderId, readyBy).getId();
    long again = deliveryService.schedule(orderId, readyBy).getId();

    assertEquals(driverId, again);
    int actions = transactionTemplate.execute(s -> driverService.findDriverById(driverId).actionsForDelivery(orderId).size());
    assertEquals(2, actions);
  }
}
