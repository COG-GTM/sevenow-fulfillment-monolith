package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.common.UnsupportedStateTransitionException;
import com.sevenow.fulfillment.domain.Order;
import com.sevenow.fulfillment.domain.OrderState;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.CUSTOMER_ID;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.slurpeeLineItems;
import static com.sevenow.fulfillment.orderservice.StoreMother.SEVEN_ELEVEN_STORE;
import static org.junit.Assert.assertEquals;

public class OrderTest {

  private Order order;

  @Before
  public void setUp() {
    order = new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, slurpeeLineItems());
  }

  @Test
  public void shouldWalkThroughPickingAndPackedToDelivered() {
    assertEquals(OrderState.APPROVED, order.getOrderState());

    order.acceptTicket(LocalDateTime.now().plusMinutes(30));
    assertEquals(OrderState.ACCEPTED, order.getOrderState());

    order.notePicking();
    assertEquals(OrderState.PICKING, order.getOrderState());

    order.notePacked();
    assertEquals(OrderState.PACKED, order.getOrderState());

    order.noteReadyForPickup();
    assertEquals(OrderState.READY_FOR_PICKUP, order.getOrderState());

    order.notePickedUp();
    assertEquals(OrderState.PICKED_UP, order.getOrderState());

    order.noteDelivered();
    assertEquals(OrderState.DELIVERED, order.getOrderState());
  }

  @Test(expected = UnsupportedStateTransitionException.class)
  public void shouldNotPackBeforePicking() {
    order.acceptTicket(LocalDateTime.now().plusMinutes(30));
    order.notePacked();
  }

  @Test(expected = UnsupportedStateTransitionException.class)
  public void shouldNotBeReadyForPickupBeforePacked() {
    order.acceptTicket(LocalDateTime.now().plusMinutes(30));
    order.notePicking();
    order.noteReadyForPickup();
  }

  @Test(expected = UnsupportedStateTransitionException.class)
  public void shouldNotStartPickingBeforeAccepted() {
    order.notePicking();
  }

  @Test(expected = UnsupportedStateTransitionException.class)
  public void shouldNotCancelOncePicking() {
    order.acceptTicket(LocalDateTime.now().plusMinutes(30));
    order.notePicking();
    order.cancel();
  }
}
