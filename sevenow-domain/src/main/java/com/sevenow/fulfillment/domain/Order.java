package com.sevenow.fulfillment.domain;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.common.UnsupportedStateTransitionException;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import static com.sevenow.fulfillment.domain.OrderState.*;


@Entity
@Table(name = "orders")
@Access(AccessType.FIELD)
@DynamicUpdate
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  private Long version;

  @Enumerated(EnumType.STRING)
  private OrderState orderState;

  private Long customerId;

  @ManyToOne(fetch = FetchType.LAZY)
  private Store store;

  @Embedded
  private OrderLineItems orderLineItems;

  @Embedded
  private DeliveryInformation deliveryInformation;

  @Embedded
  private PaymentInformation paymentInformation;

  @Embedded
  @AttributeOverride(name="amount", column = @Column(name="order_minimum"))
  private Money orderMinimum = Money.ZERO;

  private LocalDateTime readyBy;
  private LocalDateTime acceptTime;
  private LocalDateTime pickingTime;
  private LocalDateTime packedTime;
  private LocalDateTime readyForPickupTime;
  private LocalDateTime pickedUpTime;
  private LocalDateTime deliveredTime;

  @ManyToOne
  private Driver assignedDriver;

  private Order() {
  }

  public Order(long customerId, Store store, List<OrderLineItem> orderLineItems) {
    this(customerId, store, orderLineItems, Money.ZERO);
  }

  public Order(long customerId, Store store, List<OrderLineItem> orderLineItems, Money orderMinimum) {
    if (orderLineItems == null || orderLineItems.isEmpty()) {
      throw new InvalidOrderLineItemsException("Order must contain at least one line item");
    }
    this.customerId = customerId;
    this.store = store;
    this.orderLineItems = new OrderLineItems(orderLineItems);
    this.orderLineItems.validateStock(store);
    this.orderMinimum = orderMinimum;
    if (!getOrderTotal().isGreaterThanOrEqual(orderMinimum)) {
      throw new OrderMinimumNotMetException();
    }
    this.orderState = APPROVED;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }



  public Money getOrderTotal() {
    return orderLineItems.orderTotal();
  }

  public void cancel() {

    if (orderState != APPROVED) {
      throw new UnsupportedStateTransitionException(orderState);
    }

    this.orderState = CANCELLED;

  }

  public void revise(OrderRevision orderRevision) {
    if (orderState != APPROVED) {
      throw new UnsupportedStateTransitionException(orderState);
    }

    orderLineItems.validateRevision(orderRevision, store);
    LineItemQuantityChange change = orderLineItems.lineItemQuantityChange(orderRevision);
    if (!change.newOrderTotal.isGreaterThanOrEqual(getOrderMinimum())) {
      throw new OrderMinimumNotMetException();
    }

    orderRevision.getDeliveryInformation().ifPresent(newDi -> this.deliveryInformation = newDi);

    if (!orderRevision.getRevisedLineItemQuantities().isEmpty()) {
      orderLineItems.updateLineItems(orderRevision);
    }
  }

  public Money getOrderMinimum() {
    return orderMinimum == null ? Money.ZERO : orderMinimum;
  }


  public Long getVersion() {
    return version;
  }

  public List<OrderLineItem> getLineItems() {
    return orderLineItems.getLineItems();
  }

  public OrderState getOrderState() {
    return orderState;
  }

  public Store getStore() {
    return store;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void acceptTicket(LocalDateTime readyBy) {
    if (orderState == APPROVED) {
      this.acceptTime = LocalDateTime.now();
      if (!acceptTime.isBefore(readyBy))
        throw new IllegalArgumentException("readyBy is not in the future");
      this.readyBy = readyBy;
      this.orderState = ACCEPTED;
      return;
    }
    throw new UnsupportedStateTransitionException(orderState);
  }

  public void notePicking() {
    switch (orderState) {
      case ACCEPTED:
        this.orderState = OrderState.PICKING;
        this.pickingTime = LocalDateTime.now();
        return;
      default:
        throw new UnsupportedStateTransitionException(orderState);
    }
  }

  public void notePacked() {
    switch (orderState) {
      case PICKING:
        this.orderState = OrderState.PACKED;
        this.packedTime = LocalDateTime.now();
        return;
      default:
        throw new UnsupportedStateTransitionException(orderState);
    }
  }

  public void noteReadyForPickup() {
    switch (orderState) {
      case PACKED:
        this.orderState = OrderState.READY_FOR_PICKUP;
        this.readyForPickupTime = LocalDateTime.now();
        return;
      default:
        throw new UnsupportedStateTransitionException(orderState);
    }
  }

  public void notePickedUp() {
    switch (orderState) {
      case READY_FOR_PICKUP:
        this.orderState = OrderState.PICKED_UP;
        this.pickedUpTime = LocalDateTime.now();
        return;
      default:
        throw new UnsupportedStateTransitionException(orderState);
    }
  }

  public void schedule(Driver assignedDriver) {
    this.assignedDriver = assignedDriver;
  }

  public Driver getAssignedDriver() {
    return assignedDriver;
  }

  public void noteDelivered() {
    switch (orderState) {
      case PICKED_UP:
        this.orderState = OrderState.DELIVERED;
        this.deliveredTime = LocalDateTime.now();
        return;
      default:
        throw new UnsupportedStateTransitionException(orderState);
    }
  }
}

