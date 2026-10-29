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
  private Money orderMinimum = new Money(Integer.MAX_VALUE);

  private LocalDateTime readyBy;
  private LocalDateTime acceptTime;
  private LocalDateTime pickingTime;
  private LocalDateTime packedTime;
  private LocalDateTime readyForPickupTime;
  private LocalDateTime pickedUpTime;
  private LocalDateTime deliveredTime;

  /** Driver id assigned by the delivery scheduler; a plain column, not a JPA relationship to drivers. */
  @Column(name = "assigned_driver_id")
  private Long assignedDriverId;

  private Order() {
  }

  public Order(long customerId, Store store, List<OrderLineItem> orderLineItems) {
    this.customerId = customerId;
    this.store = store;
    this.orderLineItems = new OrderLineItems(orderLineItems);
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
    if (orderState == APPROVED) {
      LineItemQuantityChange change = orderLineItems.lineItemQuantityChange(orderRevision);
      if (change.newOrderTotal.isGreaterThanOrEqual(orderMinimum)) {
        throw new OrderMinimumNotMetException();
      }
    } else {
      throw new UnsupportedStateTransitionException(orderState);
    }

    orderRevision.getDeliveryInformation().ifPresent(newDi -> this.deliveryInformation = newDi);

    if (!orderRevision.getRevisedLineItemQuantities().isEmpty()) {
      orderLineItems.updateLineItems(orderRevision);
    }

    orderRevision.getDeliveryInformation().ifPresent(newDi -> this.deliveryInformation = newDi);
    if (!orderRevision.getRevisedLineItemQuantities().isEmpty()) {
      orderLineItems.updateLineItems(orderRevision);
    }

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

  public void schedule(long assignedDriverId) {
    this.assignedDriverId = assignedDriverId;
  }

  public Long getAssignedDriverId() {
    return assignedDriverId;
  }

  public DeliveryInformation getDeliveryInformation() {
    return deliveryInformation;
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

