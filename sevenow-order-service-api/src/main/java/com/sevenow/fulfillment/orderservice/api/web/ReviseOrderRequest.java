package com.sevenow.fulfillment.orderservice.api.web;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class ReviseOrderRequest {
  @NotNull
  private Map<@NotNull String, @NotNull @Min(1) Integer> revisedLineItemQuantities;

  private ReviseOrderRequest() {
  }

  public ReviseOrderRequest(Map<String, Integer> revisedLineItemQuantities) {
    this.revisedLineItemQuantities = revisedLineItemQuantities;
  }

  public Map<String, Integer> getRevisedLineItemQuantities() {
    return revisedLineItemQuantities;
  }

  public void setRevisedLineItemQuantities(Map<String, Integer> revisedLineItemQuantities) {
    this.revisedLineItemQuantities = revisedLineItemQuantities;
  }
}
