package com.sevenow.fulfillment.orderservice.web;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.orderservice.domain.DeliveryAction;
import com.sevenow.fulfillment.domain.OrderLineItem;

import java.util.List;

public class GetOrderResponse {
  private long orderId;
  private String state;
  private Money orderTotal;
  private String storeName;
  private List<OrderLineItem> lineItems;
  private Long assignedDriver;
  private List<DeliveryAction> driverActions;

  private GetOrderResponse() {
  }

  public Long getAssignedDriver() {
    return assignedDriver;
  }

  public void setAssignedDriver(Long assignedDriver) {
    this.assignedDriver = assignedDriver;
  }

  public GetOrderResponse(long orderId, String state, Money orderTotal, String storeName, List<OrderLineItem> lineItems, Long assignedDriver, List<DeliveryAction> driverActions) {
    this.orderId = orderId;
    this.state = state;
    this.orderTotal = orderTotal;
    this.storeName = storeName;
    this.lineItems = lineItems;
    this.assignedDriver = assignedDriver;
    this.driverActions = driverActions;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getStoreName() {
    return storeName;
  }

  public List<OrderLineItem> getLineItems() {
    return lineItems;
  }

  public List<DeliveryAction> getDriverActions() {
    return driverActions;
  }

  public void setDriverActions(List<DeliveryAction> driverActions) {
    this.driverActions = driverActions;
  }
}
