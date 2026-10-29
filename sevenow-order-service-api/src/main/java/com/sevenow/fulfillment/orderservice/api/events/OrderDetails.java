package com.sevenow.fulfillment.orderservice.api.events;

import com.sevenow.fulfillment.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

public class OrderDetails {

  private List<OrderLineItemDTO> lineItems;
  private Money orderTotal;

  private long storeId;
  private long customerId;

  private OrderDetails() {
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public OrderDetails(long customerId, long storeId, List<OrderLineItemDTO> lineItems, Money orderTotal) {
    this.customerId = customerId;
    this.storeId = storeId;
    this.lineItems = lineItems;
    this.orderTotal = orderTotal;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public List<OrderLineItemDTO> getLineItems() {
    return lineItems;
  }

  public long getStoreId() {
    return storeId;
  }

  public long getCustomerId() {
    return customerId;
  }


  public void setLineItems(List<OrderLineItemDTO> lineItems) {
    this.lineItems = lineItems;
  }


  public void setStoreId(long storeId) {
    this.storeId = storeId;
  }

  public void setCustomerId(long customerId) {
    this.customerId = customerId;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }


}
