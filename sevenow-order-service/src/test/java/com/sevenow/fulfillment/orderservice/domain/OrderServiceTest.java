package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.customerservice.domain.CustomerService;
import com.sevenow.fulfillment.domain.*;
import com.sevenow.fulfillment.orderservice.web.ProductIdAndQuantity;
import com.sevenow.fulfillment.security.SevenowPrincipal;
import com.sevenow.fulfillment.security.SevenowRole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.CUSTOMER_ID;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.slurpeeLineItems;
import static com.sevenow.fulfillment.orderservice.StoreMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

  private OrderRepository orderRepository;
  private CustomerService customerService;
  private OrderService orderService;
  private DriverRepository driverRepository;

  private static final long ORDER_ID = 7L;
  private static final long DRIVER_ID = 101L;

  @Before
  public void setUp() {
    orderRepository = mock(OrderRepository.class);
    StoreRepository storeRepository = mock(StoreRepository.class);
    customerService = mock(CustomerService.class);
    driverRepository = mock(DriverRepository.class);
    when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(SEVEN_ELEVEN_STORE));
    orderService = new OrderService(orderRepository, storeRepository, Optional.empty(), customerService, driverRepository);
  }

  @After
  public void clearCaller() {
    SecurityContextHolder.clearContext();
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
  public void shouldLetOwningCustomerCancelOrder() {
    Order order = givenOrder();
    actAs(SevenowRole.CUSTOMER, CUSTOMER_ID);

    orderService.cancel(ORDER_ID);

    assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
  }

  @Test
  public void shouldRequireAuthenticatedCallerForLifecycleTransitions() {
    Order order = givenOrder();

    assertThatThrownBy(() -> orderService.cancel(ORDER_ID)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThatThrownBy(() -> orderService.accept(ORDER_ID, LocalDateTime.now().plusHours(1))).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.APPROVED);
  }

  @Test
  public void shouldNotLetOtherCustomerCancelOrReviseOrder() {
    Order order = givenOrder();
    actAs(SevenowRole.CUSTOMER, CUSTOMER_ID + 1);

    assertThatThrownBy(() -> orderService.cancel(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> orderService.reviseOrder(ORDER_ID, new OrderRevision(Optional.empty(), Collections.singletonMap(CHERRY_SLURPEE_ID, 1))))
            .isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.APPROVED);
  }

  @Test
  public void shouldNotLetStoreAssociateCancelOrder() {
    Order order = givenOrder();
    actAs(SevenowRole.STORE_ASSOCIATE, STORE_ID);

    assertThatThrownBy(() -> orderService.cancel(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.APPROVED);
  }

  @Test
  public void shouldOnlyLetFulfillingStoreAcceptAndPackOrder() {
    Order order = givenOrder();

    actAs(SevenowRole.CUSTOMER, CUSTOMER_ID);
    assertThatThrownBy(() -> orderService.accept(ORDER_ID, LocalDateTime.now().plusHours(1))).isInstanceOf(AccessDeniedException.class);
    actAs(SevenowRole.STORE_ASSOCIATE, STORE_ID + 1);
    assertThatThrownBy(() -> orderService.accept(ORDER_ID, LocalDateTime.now().plusHours(1))).isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.APPROVED);
    verify(driverRepository, never()).findAllAvailable();

    order.acceptTicket(LocalDateTime.now().plusHours(1));
    actAs(SevenowRole.DRIVER, STORE_ID);
    assertThatThrownBy(() -> orderService.notePicking(ORDER_ID)).isInstanceOf(AccessDeniedException.class);

    actAs(SevenowRole.STORE_ASSOCIATE, STORE_ID);
    orderService.notePicking(ORDER_ID);
    orderService.notePacked(ORDER_ID);
    orderService.noteReadyForPickup(ORDER_ID);
    assertThat(order.getOrderState()).isEqualTo(OrderState.READY_FOR_PICKUP);
  }

  @Test
  public void shouldOnlyLetAssignedDriverPickUpAndDeliverOrder() {
    Order order = givenOrderReadyForPickupAssignedTo(DRIVER_ID);

    actAs(SevenowRole.DRIVER, DRIVER_ID + 1);
    assertThatThrownBy(() -> orderService.notePickedUp(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    actAs(SevenowRole.STORE_ASSOCIATE, STORE_ID);
    assertThatThrownBy(() -> orderService.notePickedUp(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.READY_FOR_PICKUP);

    actAs(SevenowRole.DRIVER, DRIVER_ID);
    orderService.notePickedUp(ORDER_ID);

    actAs(SevenowRole.DRIVER, DRIVER_ID + 1);
    assertThatThrownBy(() -> orderService.noteDelivered(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.PICKED_UP);

    actAs(SevenowRole.DRIVER, DRIVER_ID);
    orderService.noteDelivered(ORDER_ID);
    assertThat(order.getOrderState()).isEqualTo(OrderState.DELIVERED);
  }

  @Test
  public void shouldNotLetAnyDriverPickUpUnassignedOrder() {
    Order order = givenOrderReadyForPickupAssignedTo(null);
    actAs(SevenowRole.DRIVER, DRIVER_ID);

    assertThatThrownBy(() -> orderService.notePickedUp(ORDER_ID)).isInstanceOf(AccessDeniedException.class);
    assertThat(order.getOrderState()).isEqualTo(OrderState.READY_FOR_PICKUP);
  }

  private Order givenOrder() {
    Order order = new Order(CUSTOMER_ID, SEVEN_ELEVEN_STORE, slurpeeLineItems());
    order.setId(ORDER_ID);
    when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    return order;
  }

  private Order givenOrderReadyForPickupAssignedTo(Long driverId) {
    Order order = givenOrder();
    order.acceptTicket(LocalDateTime.now().plusHours(1));
    order.notePicking();
    order.notePacked();
    order.noteReadyForPickup();
    if (driverId != null) {
      Driver driver = mock(Driver.class);
      when(driver.getId()).thenReturn(driverId);
      order.schedule(driver);
    }
    return order;
  }

  private static void actAs(SevenowRole role, long id) {
    SevenowPrincipal principal = new SevenowPrincipal(role.name().toLowerCase() + "-" + id, role, id);
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
  }
}
