package com.sevenow.fulfillment.orderservice.web;

public class ProductIdAndQuantity {

  private String productId;
  private int quantity;

  public String getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public ProductIdAndQuantity(String productId, int quantity) {

    this.productId = productId;
    this.quantity = quantity;

  }
}
