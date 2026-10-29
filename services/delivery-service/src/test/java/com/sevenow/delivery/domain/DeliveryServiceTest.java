package com.sevenow.delivery.domain;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeliveryServiceTest {

  private static final long ORDER_ID = 42L;
  private static final LocalDateTime READY_BY = LocalDateTime.of(2026, 10, 29, 12, 0);

  private DriverRepository driverRepository;
  private DeliveryService deliveryService;
  private Driver dana;

  @Before
  public void setUp() {
    driverRepository = mock(DriverRepository.class);
    deliveryService = new DeliveryService(driverRepository, new Random(0));
    dana = new Driver(new PersonName("Dana", "Driver"), new Address("5601 N MacArthur Blvd", null, "Irving", "TX", "75038"));
    dana.noteAvailable();
    when(driverRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
  }

  @Test
  public void shouldAssignAvailableDriverWithPickupAndDropoff() {
    when(driverRepository.findAllAvailable()).thenReturn(Collections.singletonList(dana));

    Driver assigned = deliveryService.schedule(ORDER_ID, READY_BY);

    assertSame(dana, assigned);
    List<Action> actions = assigned.actionsForDelivery(ORDER_ID);
    assertEquals(2, actions.size());
    assertEquals(ActionType.PICKUP, actions.get(0).getType());
    assertNull(actions.get(0).getTime());
    assertEquals(ActionType.DROPOFF, actions.get(1).getType());
    assertEquals(READY_BY.plusMinutes(30), actions.get(1).getTime());
    assertEquals(Long.valueOf(ORDER_ID), actions.get(1).getOrderId());
  }

  @Test
  public void shouldReturnExistingDriverWhenOrderAlreadyScheduled() {
    dana.addAction(Action.makePickup(ORDER_ID));
    when(driverRepository.findByOrderId(ORDER_ID)).thenReturn(Arrays.asList(dana));

    assertSame(dana, deliveryService.schedule(ORDER_ID, READY_BY));
    assertEquals(1, dana.actionsForDelivery(ORDER_ID).size());
    verify(driverRepository, never()).findAllAvailable();
  }

  @Test(expected = NoDriverAvailableException.class)
  public void shouldFailWhenNoDriverAvailable() {
    when(driverRepository.findAllAvailable()).thenReturn(Collections.emptyList());
    deliveryService.schedule(ORDER_ID, READY_BY);
  }
}
