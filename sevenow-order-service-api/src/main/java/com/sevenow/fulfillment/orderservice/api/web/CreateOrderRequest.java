package com.sevenow.fulfillment.orderservice.api.web;

import java.util.List;

public class CreateOrderRequest {

  private long storeId;
  private long customerId;
  private List<LineItem> lineItems;

  public CreateOrderRequest(long customerId, long storeId, List<LineItem> lineItems) {
    this.storeId = storeId;
    this.customerId = customerId;
    this.lineItems = lineItems;

  }

  private CreateOrderRequest() {
  }

  public long getStoreId() {
    return storeId;
  }

  public void setStoreId(long storeId) {
    this.storeId = storeId;
  }

  public long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(long customerId) {
    this.customerId = customerId;
  }

  public List<LineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(List<LineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public static class LineItem {

    private String productId;
    private int quantity;

    private LineItem() {
    }

    public LineItem(String productId, int quantity) {
      this.productId = productId;

      this.quantity = quantity;
    }

    public String getProductId() {
      return productId;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public void setProductId(String productId) {
      this.productId = productId;

    }

  }


}
