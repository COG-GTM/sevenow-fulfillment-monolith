package com.sevenow.fulfillment.orderservice.domain;

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
  private DeliveryScheduler deliveryScheduler;

  @Before
  public void setUp() {
    orderRepository = mock(OrderRepository.class);
    StoreRepository storeRepository = mock(StoreRepository.class);
    customerService = mock(CustomerService.class);
    deliveryScheduler = mock(DeliveryScheduler.class);
    when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(SEVEN_ELEVEN_STORE));
    orderService = new OrderService(orderRepository, storeRepository, Optional.empty(), customerService, deliveryScheduler);
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
  public void shouldScheduleDeliveryThroughDeliverySchedulerOnAccept() {
    Order order = orderService.createOrder(CUSTOMER_ID, STORE_ID,
            Collections.singletonList(new ProductIdAndQuantity(CHERRY_SLURPEE_ID, 1)));
    when(orderRepository.findById(99L)).thenReturn(Optional.of(order));
    java.time.LocalDateTime readyBy = java.time.LocalDateTime.now().plusHours(1);
    when(deliveryScheduler.scheduleDelivery(order, readyBy)).thenReturn(2L);

    orderService.accept(99L, readyBy);

    assertThat(order.getOrderState()).isEqualTo(OrderState.ACCEPTED);
    assertThat(order.getAssignedDriverId()).isEqualTo(2L);
  }
}
