package com.sevenow.fulfillment.domain;

import com.sevenow.fulfillment.common.Money;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Embeddable
public class OrderLineItems {

  @ElementCollection
  @CollectionTable(name = "order_line_items")
  private List<OrderLineItem> lineItems;

  private OrderLineItems() {
  }

  public OrderLineItems(List<OrderLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public List<OrderLineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(List<OrderLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  private boolean containsProduct(String productId) {
    return lineItems.stream().anyMatch(li -> li.getProductId().equals(productId));
  }

  private int revisedQuantity(OrderLineItem li, Map<String, Integer> revisedQuantities) {
    Integer revised = revisedQuantities.get(li.getProductId());
    return revised == null ? li.getQuantity() : revised;
  }

  /**
   * Rejects a revision that names a product not in the order, sets a quantity below 1,
   * or would order more of a product than the store has on hand.
   */
  void validateRevision(OrderRevision orderRevision, Store store) {
    Map<String, Integer> revisedQuantities = orderRevision.getRevisedLineItemQuantities();
    revisedQuantities.forEach((productId, quantity) -> {
      if (productId == null || !containsProduct(productId)) {
        throw new InvalidOrderLineItemsException("Order does not contain product " + productId);
      }
      OrderLineItem.requireValidQuantity(quantity);
    });
    Map<String, Integer> quantities = new LinkedHashMap<>();
    lineItems.forEach(li -> quantities.merge(li.getProductId(), revisedQuantity(li, revisedQuantities), Integer::sum));
    requireStock(quantities, store);
  }

  void validateStock(Store store) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    lineItems.forEach(li -> quantities.merge(li.getProductId(), li.getQuantity(), Integer::sum));
    requireStock(quantities, store);
  }

  private static void requireStock(Map<String, Integer> quantitiesByProductId, Store store) {
    List<String> insufficient = quantitiesByProductId.entrySet().stream()
            .filter(e -> e.getValue() > store.findProduct(e.getKey()).map(Product::getOnHandQuantity).orElse(0))
            .map(Map.Entry::getKey)
            .collect(toList());
    if (!insufficient.isEmpty()) {
      throw new InsufficientStockException(insufficient);
    }
  }

  void updateLineItems(OrderRevision orderRevision) {
    Map<String, Integer> revisedQuantities = orderRevision.getRevisedLineItemQuantities();
    lineItems.forEach(li -> li.setQuantity(revisedQuantity(li, revisedQuantities)));
  }

  Money orderTotal() {
    return lineItems.stream().map(OrderLineItem::getTotal).reduce(Money.ZERO, Money::add);
  }

  LineItemQuantityChange lineItemQuantityChange(OrderRevision orderRevision) {
    Map<String, Integer> revisedQuantities = orderRevision.getRevisedLineItemQuantities();
    Money currentOrderTotal = orderTotal();
    Money newOrderTotal = lineItems.stream()
            .map(li -> li.getPrice().multiply(revisedQuantity(li, revisedQuantities)))
            .reduce(Money.ZERO, Money::add);
    Money delta = newOrderTotal.add(currentOrderTotal.multiply(-1));
    return new LineItemQuantityChange(currentOrderTotal, newOrderTotal, delta);
  }
}
