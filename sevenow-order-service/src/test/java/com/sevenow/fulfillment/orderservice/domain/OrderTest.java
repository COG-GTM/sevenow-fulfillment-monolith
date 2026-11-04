package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.common.UnsupportedStateTransitionException;
import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.CUSTOMER_ID;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.slurpeeLineItems;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.SLURPEE_QUANTITY;
import static com.sevenow.fulfillment.orderservice.StoreMother.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

  private static OrderRevision revision(String productId, Integer quantity) {
    Map<String, Integer> quantities = new HashMap<>();
    quantities.put(productId, quantity);
    return new OrderRevision(Optional.empty(), quantities);
  }

  private void assertReviseRejected(OrderRevision revision, Class<? extends RuntimeException> expected) {
    try {
      order.revise(revision);
      fail("expected " + expected.getSimpleName());
    } catch (RuntimeException e) {
      assertEquals(expected, e.getClass());
    }
    assertEquals(SLURPEE_QUANTITY, order.getLineItems().get(0).getQuantity());
    assertEquals(CHERRY_SLURPEE_PRICE.multiply(SLURPEE_QUANTITY), order.getOrderTotal());
  }

  @Test
  public void shouldReviseQuantity() {
    order.revise(revision(CHERRY_SLURPEE_ID, 10));
    assertEquals(10, order.getLineItems().get(0).getQuantity());
    assertEquals(CHERRY_SLURPEE_PRICE.multiply(10), order.getOrderTotal());
  }

  @Test
  public void shouldRejectRevisionToZeroQuantity() {
    assertReviseRejected(revision(CHERRY_SLURPEE_ID, 0), InvalidOrderLineItemsException.class);
  }

  @Test
  public void shouldRejectRevisionToNegativeQuantity() {
    assertReviseRejected(revision(CHERRY_SLURPEE_ID, -1), InvalidOrderLineItemsException.class);
  }

  @Test
  public void shouldRejectRevisionToNullQuantity() {
    assertReviseRejected(revision(CHERRY_SLURPEE_ID, null), InvalidOrderLineItemsException.class);
  }

  @Test
  public void shouldRejectRevisionOfProductNotInOrder() {
    assertReviseRejected(revision("NOT-A-PRODUCT", 1), InvalidOrderLineItemsException.class);
  }

  @Test
  public void shouldRejectRevisionAboveOnHandStock() {
    assertReviseRejected(revision(CHERRY_SLURPEE_ID, CHERRY_SLURPEE_PRODUCT.getOnHandQuantity() + 1), InsufficientStockException.class);
  }

  @Test
  public void shouldLeaveUnlistedLineItemsUnchanged() {
    order.revise(new OrderRevision(Optional.empty(), Collections.emptyMap()));
    assertEquals(SLURPEE_QUANTITY, order.getLineItems().get(0).getQuantity());
  }

  @Test
  public void shouldRejectRevisionBelowOrderMinimum() {
    Order withMinimum = new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, slurpees(5), new Money("5.00"));
    try {
      withMinimum.revise(revision(CHERRY_SLURPEE_ID, 2));
      fail("expected OrderMinimumNotMetException");
    } catch (OrderMinimumNotMetException e) {
      // expected
    }
    assertEquals(5, withMinimum.getLineItems().get(0).getQuantity());
  }

  @Test
  public void shouldAllowRevisionAtOrAboveOrderMinimum() {
    Order withMinimum = new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, slurpees(5), new Money("6.57"));
    withMinimum.revise(revision(CHERRY_SLURPEE_ID, 3));
    assertEquals(CHERRY_SLURPEE_PRICE.multiply(3), withMinimum.getOrderTotal());
  }

  @Test(expected = InvalidOrderLineItemsException.class)
  public void shouldRejectNegativeLineItemQuantity() {
    new OrderLineItem(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, -1);
  }

  @Test(expected = InsufficientStockException.class)
  public void shouldRejectOrderAboveOnHandStock() {
    new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, slurpees(CHERRY_SLURPEE_PRODUCT.getOnHandQuantity() + 1));
  }

  @Test(expected = InsufficientStockException.class)
  public void shouldRejectSplitLineItemsWhoseQuantitiesOverflowInt() {
    int half = 1 << 30;
    new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, Arrays.asList(
            new OrderLineItem(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, half),
            new OrderLineItem(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, half)));
  }

  @Test(expected = InvalidOrderLineItemsException.class)
  public void shouldRejectLineItemWhoseQuantityWasNeverSet() {
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setProductId(CHERRY_SLURPEE_ID);
    lineItem.setName(CHERRY_SLURPEE);
    lineItem.setPrice(CHERRY_SLURPEE_PRICE);
    new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, Collections.singletonList(lineItem));
  }

  @Test
  public void shouldAllowRevisionsThatDoNotIncreaseQuantityAfterStockDrops() {
    Product slurpee = new Product(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, 40);
    Store store = new Store(STORE_ID, STORE_NUMBER, STORE_NAME, new StoreCatalog(Collections.singletonList(slurpee)));
    Order existing = new Order(CUSTOMER_ID, store, slurpees(5));
    slurpee.setOnHandQuantity(0);

    existing.revise(new OrderRevision(Optional.empty(), Collections.emptyMap()));
    existing.revise(revision(CHERRY_SLURPEE_ID, 5));
    existing.revise(revision(CHERRY_SLURPEE_ID, 3));
    assertEquals(3, existing.getLineItems().get(0).getQuantity());

    try {
      existing.revise(revision(CHERRY_SLURPEE_ID, 4));
      fail("expected InsufficientStockException");
    } catch (InsufficientStockException e) {
      // expected
    }
  }

  private static List<OrderLineItem> slurpees(int quantity) {
    return Collections.singletonList(new OrderLineItem(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, quantity));
  }
}
