package com.sevenow.delivery.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

/**
 * One step of a driver's plan. Refers to the monolith's order by id only — there is no
 * {@code @ManyToOne Order} (and no FK) across the service boundary.
 */
@Embeddable
public class Action {

  @Enumerated(EnumType.STRING)
  private ActionType type;
  private LocalDateTime time;

  @Column(name = "order_id")
  private Long orderId;

  private Action() {
  }

  public Action(ActionType type, long orderId, LocalDateTime time) {
    this.type = type;
    this.orderId = orderId;
    this.time = time;
  }

  public boolean actionFor(long orderId) {
    return this.orderId != null && this.orderId == orderId;
  }

  public static Action makePickup(long orderId) {
    return new Action(ActionType.PICKUP, orderId, null);
  }

  public static Action makeDropoff(long orderId, LocalDateTime deliveryTime) {
    return new Action(ActionType.DROPOFF, orderId, deliveryTime);
  }

  public ActionType getType() {
    return type;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public Long getOrderId() {
    return orderId;
  }
}
