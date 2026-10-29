package com.sevenow.fulfillment.orderservice.api.events;

public class OrderLineItemDTO {

  private int quantity;
  private String productId;
  private String name;

  public OrderLineItemDTO() {
  }


  public OrderLineItemDTO(int quantity, String productId, String name) {
    this.quantity = quantity;
    this.productId = productId;
    this.name = name;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
