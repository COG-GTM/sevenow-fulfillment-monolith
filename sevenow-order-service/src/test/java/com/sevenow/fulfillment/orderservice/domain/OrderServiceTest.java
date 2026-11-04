package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.customerservice.domain.CustomerService;
import com.sevenow.fulfillment.domain.*;
import com.sevenow.fulfillment.orderservice.web.ProductIdAndQuantity;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.CUSTOMER_ID;
import static com.sevenow.fulfillment.orderservice.StoreMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

  private OrderRepository orderRepository;
  private CustomerService customerService;
  private OrderService orderService;

  @Before
  public void setUp() {
    orderRepository = mock(OrderRepository.class);
    StoreRepository storeRepository = mock(StoreRepository.class);
    customerService = mock(CustomerService.class);
    DriverRepository driverRepository = mock(DriverRepository.class);
    when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(SEVEN_ELEVEN_STORE));
    orderService = new OrderService(orderRepository, storeRepository, Optional.empty(), customerService, driverRepository, Money.ZERO);
  }

  @Test
  public void shouldCreateOrderForInStockProduct() {
    Order order = orderService.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 3)));

    assertThat(order.getOrderState()).isEqualTo(OrderState.APPROVED);
    assertThat(order.getLineItems()).hasSize(1);
    OrderLineItem li = order.getLineItems().get(0);
    assertThat(li.getProductId()).isEqualTo(CHERRY_SLURPEE_ID);
    assertThat(li.isSubstituted()).isFalse();
    assertThat(order.getOrderTotal()).isEqualTo(CHERRY_SLURPEE_PRICE.multiply(3));
    verify(customerService).validateOrderForCustomer(CUSTOMER_ID, order.getOrderTotal());
    verify(orderRepository).save(order);
  }

  @Test
  public void shouldSubstituteOutOfStockProductWithConfiguredSubstitute() {
    Order order = orderService.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity(PEPPERONI_PIZZA_ID, 2)));

    OrderLineItem li = order.getLineItems().get(0);
    assertThat(li.getProductId()).isEqualTo(CHEESE_PIZZA_ID);
    assertThat(li.getName()).isEqualTo(CHEESE_PIZZA);
    assertThat(li.getSubstitutedFromProductId()).isEqualTo(PEPPERONI_PIZZA_ID);
    assertThat(li.getQuantity()).isEqualTo(2);
    assertThat(order.getOrderTotal()).isEqualTo(CHEESE_PIZZA_PRICE.multiply(2));
  }

  @Test
  public void shouldRejectOutOfStockProductWithoutSubstitute() {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID, Arrays.asList(
              new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 1),
              new ProductIdAndQuantity(CHICKEN_WINGS_ID, 1)));
      fail("expected ProductOutOfStockException");
    } catch (ProductOutOfStockException e) {
      assertThat(e.getRejectedProductIds()).containsExactly(CHICKEN_WINGS_ID);
    }
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test(expected = InvalidProductIdException.class)
  public void shouldRejectUnknownProduct() {
    orderService.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity("NOT-A-PRODUCT", 1)));
  }

  @Test
  public void shouldRejectZeroQuantity() {
    assertCreateRejected(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 0), InvalidOrderLineItemsException.class);
  }

  @Test
  public void shouldRejectNegativeQuantity() {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID, Arrays.asList(
              new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 1),
              new ProductIdAndQuantity(CHEESE_PIZZA_ID, -1)));
      fail("expected InvalidOrderLineItemsException");
    } catch (InvalidOrderLineItemsException e) {
      // expected
    }
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  public void shouldRejectEmptyOrder() {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID, Collections.emptyList());
      fail("expected InvalidOrderLineItemsException");
    } catch (InvalidOrderLineItemsException e) {
      // expected
    }
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  public void shouldRejectQuantityAboveOnHandStock() {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID,
              Collections.singletonList(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, CHERRY_SLURPEE_PRODUCT.getOnHandQuantity() + 1)));
      fail("expected InsufficientStockException");
    } catch (InsufficientStockException e) {
      assertThat(e.getProductIds()).containsExactly(CHERRY_SLURPEE_ID);
    }
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  public void shouldAllowQuantityEqualToOnHandStock() {
    Order order = orderService.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity(CHEESE_PIZZA_ID, CHEESE_PIZZA_PRODUCT.getOnHandQuantity())));
    assertThat(order.getLineItems().get(0).getQuantity()).isEqualTo(CHEESE_PIZZA_PRODUCT.getOnHandQuantity());
  }

  @Test
  public void shouldRejectSplitLineItemsThatTogetherExceedStock() {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID, Arrays.asList(
              new ProductIdAndQuantity(PEPPERONI_PIZZA_ID, 4),
              new ProductIdAndQuantity(CHEESE_PIZZA_ID, 3)));
      fail("expected InsufficientStockException");
    } catch (InsufficientStockException e) {
      assertThat(e.getProductIds()).containsExactly(CHEESE_PIZZA_ID);
    }
  }

  @Test
  public void shouldRejectOrderBelowConfiguredMinimum() {
    StoreRepository storeRepository = mock(StoreRepository.class);
    when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(SEVEN_ELEVEN_STORE));
    OrderService withMinimum = new OrderService(orderRepository, storeRepository, Optional.empty(), customerService,
            mock(DriverRepository.class), new Money("5.00"));
    try {
      withMinimum.createOrder(CUSTOMER_ID, STORE_ID, Collections.singletonList(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 2)));
      fail("expected OrderMinimumNotMetException");
    } catch (OrderMinimumNotMetException e) {
      // expected
    }
    assertThat(withMinimum.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 3))).getOrderTotal())
            .isEqualTo(CHERRY_SLURPEE_PRICE.multiply(3));
  }

  private void assertCreateRejected(ProductIdAndQuantity lineItem, Class<? extends RuntimeException> expected) {
    try {
      orderService.createOrder(CUSTOMER_ID, STORE_ID, Collections.singletonList(lineItem));
      fail("expected " + expected.getSimpleName());
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(expected);
    }
    verify(orderRepository, never()).save(any(Order.class));
  }
}
